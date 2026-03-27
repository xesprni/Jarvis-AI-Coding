package com.qifu.ui.settings.agent

import com.qifu.utils.AgentConfig

/**
 * 智能体保存范围枚举
 * - USER: 全局范围，保存到用户目录 (~/.jarvis/agents)
 * - PROJECT: 项目范围，保存到项目目录 (.jarvis/agents)
 */
enum class SaveScope(private val display: String) {
    USER("全局 (~/.jarvis/agents)"),
    PROJECT("项目 (.jarvis/agents)");

    override fun toString(): String = display

    /** 检查当前范围是否与给定的 Location 匹配 */
    fun matches(location: AgentConfig.Scope): Boolean =
        (this == USER && location == AgentConfig.Scope.USER) ||
        (this == PROJECT && location == AgentConfig.Scope.PROJECT)
}

/**
 * AI 生成的智能体配置数据类
 *
 * @property name 智能体标识
 * @property description 使用场景描述
 * @property tools 工具权限列表
 * @property systemPrompt 系统提示词
 */
data class GeneratedAgent(
    val name: String,
    val description: String,
    val tools: List<String>,
    val systemPrompt: String
)

/**
 * 智能体表单状态，用于检测未保存更改
 */
data class AgentFormState(
    val name: String,
    val desc: String,
    val prompt: String,
    val scope: SaveScope,
    val tools: Any
)
