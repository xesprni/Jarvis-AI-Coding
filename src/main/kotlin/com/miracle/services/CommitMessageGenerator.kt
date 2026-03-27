package com.miracle.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.miracle.config.JarvisCoreSettings
import com.miracle.utils.GitUtil
import dev.langchain4j.data.message.UserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI 提交信息生成服务
 * 
 * 根据当前 Git 变更自动生成规范的 Commit Message
 */
object CommitMessageGenerator {
    
    private val LOG = Logger.getInstance(CommitMessageGenerator::class.java)
    
    /**
     * 生成 Commit Message
     * 
     * @param project 当前项目
     * @param commitType 提交类型 (如 "feat", "fix", "docs" 等)
     * @param issueKey Jira 任务编号 (如 "NREQUEST-10086")
     * @return 生成的 Commit Message,如果失败返回 null
     */
    suspend fun generateCommitMessage(
        project: Project,
        commitType: String? = null,
        issueKey: String? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 获取当前的 Git 变更 diff
                val gitDiff = GitUtil.getCurrentChanges(project)
                if (gitDiff.isNullOrBlank()) {
                    LOG.warn("无法获取 Git 变更信息")
                    return@withContext null
                }
                
                // 2. 构建提示词
                val prompt = buildPrompt(gitDiff, commitType, issueKey)
                
                // 3. 调用 AI 模型生成
                val modelId = JarvisCoreSettings.getInstance().selectedChatModelId
                    ?: throw IllegalStateException("未配置可用模型")
                
                val messages = listOf(UserMessage.from(prompt))
                
                val response = chatCompletion(
                    messages = messages,
                    systemPrompt = emptyList(),
                    tools = emptyList(),
                    options = QueryLLMOptions(
                        model = modelId,
                        streaming = false
                    )
                )
                
                // 4. 提取生成的 Commit Message
                val generatedMessage = response.aiMessage().text().trim()
                LOG.info("AI 生成的 Commit Message: $generatedMessage")
                
                return@withContext generatedMessage
                
            } catch (e: Exception) {
                LOG.warn("生成 Commit Message 失败", e)
                return@withContext null
            }
        }
    }
    
    /**
     * 构建 AI 提示词
     */
    private fun buildPrompt(
        gitDiff: String,
        commitType: String?,
        issueKey: String?
    ): String {
        val promptBuilder = StringBuilder()
        
        promptBuilder.append("你是一个专业的代码提交信息生成助手。请根据以下 Git Diff 生成简洁明了的 Commit Message。\n\n")
        
        // 添加规范要求
        promptBuilder.append("要求:\n")
        promptBuilder.append("1. 使用中文描述变更内容\n")
        promptBuilder.append("2. 描述要简洁明了,突出核心变更\n")
        promptBuilder.append("3. 只输出提交信息本身,不要包含其他解释\n")
        
        // 如果指定了提交类型和任务编号,则添加格式要求
        if (!commitType.isNullOrBlank() && !issueKey.isNullOrBlank()) {
            promptBuilder.append("4. 格式: 直接描述变更内容即可 (不需要包含类型和任务编号,这些会自动添加)\n")
        } else {
            promptBuilder.append("4. 如果变更较多,可以用简短的一句话总结主要变更\n")
        }
        
        promptBuilder.append("\n")
        
        // 添加 Git Diff
        promptBuilder.append("Git Diff:\n")
        promptBuilder.append("```diff\n")
        promptBuilder.append(gitDiff)
        promptBuilder.append("\n```\n\n")
        
        promptBuilder.append("请生成 Commit Message:")
        
        return promptBuilder.toString()
    }
}
