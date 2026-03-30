package com.miracle.listener

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.miracle.external.RipGrepUtil
import com.miracle.services.AgentService

/**
 * 项目启动活动，在项目打开时提前初始化 [AgentService] 并确保 RipGrep 工具已安装。
 */
class JarvisProjectStartupActivity : ProjectActivity {

    /**
     * 项目启动时执行初始化操作。
     *
     * @param project 当前打开的项目
     */
    override suspend fun execute(project: Project) {
        project.service<AgentService>()
        runCatching { RipGrepUtil.ensureInstalled() }
    }
}
