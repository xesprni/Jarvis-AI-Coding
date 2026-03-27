package com.qifu.listener

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.qifu.agent.mcp.McpClientHub


class JarvisAppLifeListener : AppLifecycleListener {

    companion object {
        val LOG = Logger.getInstance(JarvisAppLifeListener::class.java)
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        LOG.info("IDE is closing, cleaning up...")
        shutdownMcpClients()
//        PersistentShell.restart()
    }

    override fun projectFrameClosed() {
        super.projectFrameClosed()
        LOG.info("PROJECT FRAME is closing, cleaning up...")
        shutdownMcpClients()
    }

    private fun shutdownMcpClients() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (!project.isDisposed) {
                runCatching { McpClientHub.getInstance(project).shutdown() }
                    .onFailure { LOG.warn("Failed to shutdown MCP clients for project ${project.name}", it) }
            }
        }
    }
}
