package com.miracle.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.miracle.utils.AgentLoader
import com.miracle.utils.SkillLoader
import kotlinx.coroutines.Dispatchers

@Service(Service.Level.PROJECT)
class AgentService(project: Project) : Disposable {

    val agentLoader = AgentLoader()
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

    override fun dispose() {
        agentLoader.stopWatcher()
        skillLoader.stopWatcher()
    }

}
