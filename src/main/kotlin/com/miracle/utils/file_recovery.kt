package com.miracle.utils

import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.TaskState
import kotlin.math.ceil
import kotlin.math.min

private var LOG = Logger.getInstance("com.miracle.utils.file_recovery")

/**
 * File recovery configuration for auto-compact feature
 * These limits ensure recovered files don't overwhelm the compressed context
 */
const val  MAX_FILES_TO_RECOVER = 5
const val MAX_TOKENS_PER_FILE = 10_000
const val MAX_TOTAL_FILE_TOKENS = 50_000


/**
 * Selects and reads recently accessed files for context recovery
 *
 * During auto-compact, this function preserves development context by:
 * - Selecting files based on recent access patterns
 * - Enforcing token budgets to prevent context bloat
 * - Truncating large files while preserving essential content
 *
 * @returns Array of file data with content, token counts, and truncation flags
 */
data class SelectAndReadFileResult (
    val path: String,
    val content: String,
    val tokens: Int,
    val truncated: Boolean
)
fun selectAndReadFiles(taskState: TaskState): List<SelectAndReadFileResult> {
    val importantFiles = taskState.fileFreshnessService!!.getImportantFiles(MAX_FILES_TO_RECOVER)

    var totalTokens = 0
    val result = mutableListOf<SelectAndReadFileResult>()
    for (fileInfo in importantFiles) {
        try {
            val content = readTextContent(fileInfo.path)
            val estimatedTokens = ceil(content.length * 0.25).toInt()

            var finalContent = content
            var truncated = false
            if (estimatedTokens > MAX_TOKENS_PER_FILE) {
                val maxChars = (MAX_TOKENS_PER_FILE / 0.25).toInt()
                finalContent = content.take(maxChars)
                truncated = true
            }
            val finalTokens = min(estimatedTokens, MAX_TOKENS_PER_FILE)
            if (totalTokens + finalTokens > MAX_TOTAL_FILE_TOKENS) {
                break
            }
            totalTokens += finalTokens
            result.add(SelectAndReadFileResult(fileInfo.path, finalContent, finalTokens, truncated))
        } catch (e: Exception) {
            // Ignore files that cannot be read
            LOG.warn("Failed to read file for recovery:${fileInfo.path}", e)
        }
    }
    return result
}