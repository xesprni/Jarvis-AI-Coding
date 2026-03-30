package com.miracle.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.miracle.utils.AgentLoader
import com.miracle.utils.SkillLoader
import kotlinx.coroutines.Dispatchers

/**
 * Agent 和 Skill 加载与管理服务，负责初始化并监控 Agent 与 Skill 配置的变更
 */
@Service(Service.Level.PROJECT)
class AgentService(project: Project) : Disposable {

    /** Agent 配置加载器，管理 subagent 的加载与热更新 */
    val agentLoader = AgentLoader()
    /** Skill 配置加载器，管理 skill 的加载与热更新 */
    val skillLoader = SkillLoader()

    init {
        agentLoader.startWatcher()
        skillLoader.startWatcher()

        // 等待项目索引完成后再加配置
        DumbService.getInstance(project).runWhenSmart {
            with(Dispatchers.Default) {
                // 初始化时同步已激活的subagent配置
                agentLoader.loadAllAgents(isRecordConfig = false, project = project)
                skillLoader.loadAllSkills(isRecordConfig = false, project = project)
            }
        }
    }

    /**
     * 释放资源，停止所有文件监控
     */
    override fun dispose() {
        agentLoader.stopWatcher()
        skillLoader.stopWatcher()
    }

}
