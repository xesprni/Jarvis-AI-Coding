package com.qifu.ui.settings.agent

import com.intellij.openapi.diagnostic.Logger
import com.qifu.agent.tool.ToolRegistry
import com.qifu.services.QueryLLMOptions
import com.qifu.services.chatCompletion
import com.qifu.services.loadModelConfigs
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings
import dev.langchain4j.data.message.UserMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * 智能体 AI 生成器
 *
 * 负责使用 LLM 智能生成智能体配置，包括：
 * - 调用 LLM 接口获取生成结果
 * - 解析 LLM 返回的 JSON 格式配置
 * - 提供生成提示词模板
 */
object AgentAIGenerator {
    private val log = Logger.getInstance(AgentAIGenerator::class.java)

    /** 默认使用的 LLM 模型 ID */
    private const val DEFAULT_MODEL_ID = "JARVIS_360/360-qwen3-coder-480b-a35b"

    /**
     * 使用 AI 智能生成智能体配置
     *
     * @param requirement 用户输入的智能体需求描述
     * @return 生成的智能体配置
     * @throws Exception 如果生成失败
     */
    suspend fun generate(requirement: String): GeneratedAgent {
        // 获取配置的模型 ID，优先使用用户设置
        val modelId = try {
            val models = loadModelConfigs()
            ChatxApplicationSettings.settings().chatModelId
                ?: models.keys.firstOrNull()
                ?: DEFAULT_MODEL_ID
        } catch (e: Exception) {
            log.warn("Failed to load models for agent generation, fallback to default", e)
            ChatxApplicationSettings.settings().chatModelId ?: DEFAULT_MODEL_ID
        }

        // 调用 LLM 生成智能体配置
        val response = chatCompletion(
            messages = listOf(
                UserMessage.userMessage(buildGenerationPrompt(requirement))
            ),
            systemPrompt = listOf(GENERATION_SYSTEM_PROMPT),
            options = QueryLLMOptions(model = modelId),
        )

        // 解析 LLM 返回的 JSON
        val raw = response.aiMessage().text() ?: ""
        val jsonText = extractJson(raw)
        return parseGeneratedAgent(jsonText)
    }

    /**
     * 同步版本的生成方法（用于后台线程调用）
     */
    fun generateBlocking(requirement: String): GeneratedAgent = runBlocking {
        generate(requirement)
    }

    /**
     * 构建生成提示词
     */
    private fun buildGenerationPrompt(requirement: String): String {
        return """
用户需求:
$requirement
现有工具集合:
${ToolRegistry.getAll().map { it.key to it.value.getToolSpecification() }}

请输出一个 JSON：
{
  "name": "kebab-case 标识",
  "description": "一句中文描述",
  "tools": ["*", "read", "glob"],
  "systemPrompt": "完整的系统提示词"
}
        """.trimIndent()
    }

    /**
     * 从 LLM 响应中提取 JSON 内容
     *
     * 处理以下情况：
     * 1. 带有 Markdown 代码块包裹的 JSON（```json ... ```）
     * 2. 直接的 JSON 对象
     * 3. JSON 前后有多余文本
     *
     * @param raw LLM 原始响应文本
     * @return 提取出的 JSON 字符串
     */
    fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        // 处理 Markdown 代码块
        if (trimmed.startsWith("```")) {
            val withoutFence = trimmed.removePrefix("```").trimStart()
            val cleaned = if (withoutFence.startsWith("json")) {
                withoutFence.substringAfter("\n", "").trimStart()
            } else {
                withoutFence
            }
            return cleaned.substringBeforeLast("```").trim()
        }
        // 直接查找 JSON 对象边界
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start in 0..<end) trimmed.substring(start, end + 1) else trimmed
    }

    /**
     * 解析 AI 生成的智能体 JSON 配置
     *
     * 支持的字段别名：
     * - name / agentType
     * - description / whenToUse
     * - systemPrompt / prompt
     *
     * @param jsonText JSON 格式的智能体配置
     * @return 解析后的 GeneratedAgent 对象
     * @throws IllegalArgumentException 如果 JSON 格式无效或缺少必要字段
     */
    fun parseGeneratedAgent(jsonText: String): GeneratedAgent {
        val element = Json.parseToJsonElement(jsonText)
        val obj = element as? JsonObject ?: throw IllegalArgumentException("未能解析生成结果，请重试")

        // 支持多个字段名别名
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: obj["agentType"]?.jsonPrimitive?.contentOrNull ?: ""
        val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: obj["whenToUse"]?.jsonPrimitive?.contentOrNull ?: ""

        // tools 可以是字符串或数组
        val tools = when (val toolsElement = obj["tools"]) {
            is JsonArray -> toolsElement.mapNotNull { it.jsonPrimitive.contentOrNull }.ifEmpty { listOf("*") }
            is JsonElement -> listOf(toolsElement.jsonPrimitive.content)
            else -> listOf("*")
        }

        val prompt = obj["systemPrompt"]?.jsonPrimitive?.contentOrNull ?: obj["prompt"]?.jsonPrimitive?.contentOrNull
            ?: ""

        if (name.isBlank() || prompt.isBlank()) {
            throw IllegalArgumentException("生成结果缺少必要字段")
        }
        return GeneratedAgent(
            name = name,
            description = description.ifBlank { "待补充" },
            tools = tools,
            systemPrompt = prompt
        )
    }

    /** AI 生成智能体时使用的系统提示词 */
    private val GENERATION_SYSTEM_PROMPT = """
        你是一个智能体配置生成器，负责根据用户需求生成智能体的 JSON 配置。
        请确保生成的 JSON 格式正确，并包含以下字段：
        - name: 智能体的唯一标识，使用 kebab-case 格式
        - description: 智能体的简短中文描述，说明何时使用该智能体
        - tools: 智能体可使用的工具列表，支持 "*" 表示所有工具
        - systemPrompt: 智能体的系统提示词，需完整描述智能体的行为和职责
        请严格按照 JSON 格式输出结果，避免包含多余的文本或解释。
        关于systemPrompt字段，请确保它足够详细，以指导智能体在各种情境下的行为。使用markdown语法，但要确保整体是一个合法的JSON格式。
    ""${'"'}.trimIndent()
    """.trimIndent()
}
