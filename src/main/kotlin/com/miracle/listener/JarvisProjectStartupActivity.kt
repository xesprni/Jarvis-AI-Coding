package com.miracle.listener

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.miracle.external.RipGrepUtil
import com.miracle.services.AgentService

class JarvisProjectStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 服务默认是懒加载，这里要提前加载
        project.service<AgentService>()
        runCatching { RipGrepUtil.ensureInstalled() }
    }
}
