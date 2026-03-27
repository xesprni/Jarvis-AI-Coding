package com.qifu.utils

import com.intellij.openapi.diagnostic.Logger
import com.qifu.agent.TaskState
import com.qifu.services.QueryLLMOptions
import com.qifu.services.chatCompletion
import com.qifu.services.loadModelConfigs
import com.qifu.utils.extensions.withAttribute
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.model.output.TokenUsage
import kotlin.math.max
import kotlin.math.roundToInt

private var LOG = Logger.getInstance("com.qifu.utils.auto_compact")
/**
 * Threshold ratio for triggering automatic context compression
 * When context usage exceeds 92% of the model's limit, auto-compact activates
 */
const val AUTO_COMPACT_THRESHOLD_RATIO = 0.8

/**
 * Retrieves the context length for the main model that should execute compression
 * Uses ModelManager to get the current model's context length
 */
suspend fun getCompressionModelContextLimit(modelId: String): Int {
    return loadModelConfigs()[modelId]?.contextTokens ?: 128_000
}

const val COMPRESSION_PROMPT = """Please provide a comprehensive summary of our conversation structured as follows:

## Technical Context
Development environment, tools, frameworks, and configurations in use. Programming languages, libraries, and technical constraints. File structure, directory organization, and project architecture.

## Project Overview  
Main project goals, features, and scope. Key components, modules, and their relationships. Data models, APIs, and integration patterns.

## Code Changes
Files created, modified, or analyzed during our conversation. Specific code implementations, functions, and algorithms added. Configuration changes and structural modifications.

## Debugging & Issues
Problems encountered and their root causes. Solutions implemented and their effectiveness. Error messages, logs, and diagnostic information.

## Current Status
What we just completed successfully. Current state of the codebase and any ongoing work. Test results, validation steps, and verification performed.

## Pending Tasks
Immediate next steps and priorities. Planned features, improvements, and refactoring. Known issues, technical debt, and areas needing attention.

## User Preferences
Coding style, formatting, and organizational preferences. Communication patterns and feedback style. Tool choices and workflow preferences.

## Key Decisions
Important technical decisions made and their rationale. Alternative approaches considered and why they were rejected. Trade-offs accepted and their implications.

Focus on information essential for continuing the conversation effectively, including specific details about code, files, errors, and plans."""





/**
 * Main entry point for automatic context compression
 *
 * This function is called before each query to check if the conversation
 * has grown too large and needs compression. When triggered, it:
 * - Generates a structured summary of the conversation using the main model
 * - Recovers recently accessed files to maintain development context
 * - Resets conversation state while preserving essential information
 *
 * Uses the main model for compression tasks to ensure high-quality summaries
 *
 * @param messages Current conversation messages
 * @param toolUseContext Execution context with model and tool configuration
 * @returns Updated messages (compressed if needed) and compression status
 */
data class AutoCompactResult(val messages: List<ChatMessage>, val wasCompacted: Boolean)
suspend fun checkAutoCompact(modelId: String, messages: List<ChatMessage>, taskState: TaskState): AutoCompactResult {
    if (!shouldAutoCompact(modelId, messages)) return AutoCompactResult(messages, false)
    return try {
        LOG.info("Auto-compact triggered, conversation context compressed.")
        val compactedMessages = executeAutoCompact(modelId, messages, taskState)
        AutoCompactResult(compactedMessages, true)
    } catch (e: Exception) {
        // Graceful degradation: if auto-compact fails, drop oldest messages
        // This ensures system remains functional even if compression encounters issues
        LOG.warn("Auto-compact failed, dropping oldest messages as fallback:", e)
        val fallbackMessages = dropOldestMessages(messages)
        val wasCompacted = fallbackMessages.size != messages.size
        AutoCompactResult(fallbackMessages, wasCompacted)
    }
}

suspend fun forceCompact(modelId: String, messages: List<ChatMessage>, taskState: TaskState): AutoCompactResult {
    val compactedMessages = executeAutoCompact(modelId, messages, taskState)
    return AutoCompactResult(compactedMessages, true)
}

/**
 * Calculates context usage thresholds based on the main model's capabilities
 * Uses the main model context length since compression tasks require a capable model
 */
private suspend fun calculateThresholds(modelId: String, tokenCount: Int): Map<String, Any> {
    val contextLimit = getCompressionModelContextLimit(modelId)
    val autoCompactThreshold = contextLimit * AUTO_COMPACT_THRESHOLD_RATIO
    return mapOf(
        "isAboveAutoCompactThreshold" to (tokenCount >= autoCompactThreshold),
        "percentUsed" to ((tokenCount.toDouble() / contextLimit) * 100).roundToInt(),
        "tokensRemaining " to max(0, (autoCompactThreshold - tokenCount).toInt()),
        "contextLimit " to contextLimit,
    )
}

/**
 * Determines if auto-compact should trigger based on token usage
 * Uses the main model context limit since compression requires a capable model
 */
private suspend fun shouldAutoCompact(modelId: String, messages: List<ChatMessage>): Boolean {
    if (messages.size < 3) return false

    val tokenCount = countTokens(messages)
    val isAboveAutoCompactThreshold = calculateThresholds(modelId, tokenCount)["isAboveAutoCompactThreshold"] as Boolean
    return isAboveAutoCompactThreshold
}

/**
 * Executes the conversation compression process using the main model
 *
 * This function generates a comprehensive summary using the main model
 * which is better suited for complex summarization tasks. It also
 * automatically recovers important files to maintain development context.
 */
private suspend fun executeAutoCompact(modelId: String, messages: List<ChatMessage>, taskState: TaskState): List<ChatMessage> {
    val response = chatCompletion(
        messages = messages + listOf(userMessage(COMPRESSION_PROMPT)),
        systemPrompt = listOf("You are a helpful AI assistant tasked with creating comprehensive conversation summaries that preserve all essential context for continuing development work."),
        options = QueryLLMOptions(model = modelId)
    )
    val summary = response.aiMessage().text()
    if (summary.isNullOrEmpty()) throw Exception("Failed to generate conversation summary - response did not contain valid text content")

    val tokenUsage = response.metadata().tokenUsage()
    val compactTokenUsage = TokenUsage(0, tokenUsage.outputTokenCount(), tokenUsage.outputTokenCount())  // 压缩后忽略之前的token
    val aiMessage = response.aiMessage().withAttribute(TOKEN_USAGE_KEY, mapOf(
        "inputTokenCount" to compactTokenUsage.inputTokenCount(),
        "outputTokenCount" to compactTokenUsage.outputTokenCount(),
        "totalTokenCount" to compactTokenUsage.totalTokenCount(),
    ))

    // Automatic file recovery: preserve recently accessed development files
    // This maintains coding context even after conversation compression
    val recoveredFiles = selectAndReadFiles(taskState)
    val compactedMessages = mutableListOf<ChatMessage>(
        userMessage("Context automatically compressed due to token limit. Essential information preserved."),
        aiMessage,
    )
    // Append recovered files to maintain development workflow continuity
    // Files are prioritized by recency and importance, with strict token limits
    recoveredFiles.forEach { file ->
        val contentWithLines = addLineNumbers(file.content, 1)
        val recoveryMessage = userMessage("**Recovered File: ${file.path}**\n\n```\n$contentWithLines\n```\n\n*Automatically recovered (${file.tokens} tokens)${if (file.truncated) " [truncated]" else ""}*")
        compactedMessages.add(recoveryMessage)
    }
    // State cleanup to ensure fresh context after compression
    taskState.fileFreshnessService!!.resetSession()

    return compactedMessages
}

/**
 * Fallback function to drop oldest messages when compression fails
 * Removes the oldest messages (including ToolExecutionResultMessage) to reduce token usage
 * Preserves the most recent context while ensuring system remains functional
 */
private fun dropOldestMessages(messages: List<ChatMessage>): List<ChatMessage> {
    if (messages.size <= 16) {
        LOG.warn("Fallback would leave insufficient messages, keeping recent context")
        return messages
    }
    
    // Drop messages iteratively until conditions are met
    var remainingMessages = messages.drop(8)
    var droppedCount = 8
    
    // Continue dropping if the remaining messages start with ToolExecutionResultMessage
    while (remainingMessages.isNotEmpty() && remainingMessages.first() is ToolExecutionResultMessage) {
        remainingMessages = remainingMessages.drop(1)
        droppedCount++
        LOG.info("Dropped additional message to avoid ToolMessage at start, total dropped: $droppedCount")
    }
    
    if (remainingMessages.isEmpty()) {
        LOG.warn("Fallback would leave insufficient messages, keeping recent context")
        return messages
    }
    
    LOG.info("Dropped $droppedCount messages as fallback, remaining: ${remainingMessages.size}")
    val fallbackNotification = userMessage("⚠️ **Context Management Fallback**: Due to token limits and compression failure, the oldest conversation messages (including tool results) have been automatically removed to continue the conversation. Key context may have been lost.")
    
    return listOf(fallbackNotification) + remainingMessages
}
