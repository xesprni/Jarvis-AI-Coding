package com.miracle.utils

import com.intellij.openapi.project.Project

/**
 * Agent 配置追踪工具（当前为空实现，远程配置上传已移除）
 */
object AgentConfigTraceUtil {
    /**
     * 追踪 Agent 配置信息
     * @param agentConfig Agent 配置
     * @param path 配置路径
     * @param project 当前项目
     */
    fun traceAgentConfig(agentConfig: AgentConfig, path: String, project: Project) {
        // Remote config upload was removed.
    }

    /**
     * 追踪技能配置信息
     * @param scope 技能配置作用域
     * @param configs 配置列表
     * @param project 当前项目
     */
    fun traceSkillConfig(scope: SkillConfig.Scope, configs: List<Any>, project: Project) {
        // Remote config upload was removed.
    }
}
