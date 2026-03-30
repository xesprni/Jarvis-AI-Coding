package com.miracle.listener

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.miracle.agent.mcp.McpClientHub


/**
 * 应用生命周期监听器，监听 IDE 关闭和项目窗口关闭事件，
 * 负责在关闭时清理 MCP 客户端等资源。
 */
class JarvisAppLifeListener : AppLifecycleListener {

    companion object {
        /** 日志记录器 */
        val LOG = Logger.getInstance(JarvisAppLifeListener::class.java)
    }

    /**
     * IDE 即将关闭时的回调，执行资源清理。
     *
     * @param isRestart 是否为重启操作
     */
    override fun appWillBeClosed(isRestart: Boolean) {
        LOG.info("IDE is closing, cleaning up...")
        shutdownMcpClients()
    }

    /**
     * 项目窗口关闭时的回调，执行资源清理。
     */
    override fun projectFrameClosed() {
        super.projectFrameClosed()
        LOG.info("PROJECT FRAME is closing, cleaning up...")
        shutdownMcpClients()
    }

    /**
     * 关闭所有已打开项目中的 MCP 客户端连接。
     */
    private fun shutdownMcpClients() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (!project.isDisposed) {
                runCatching { McpClientHub.getInstance(project).shutdown() }
                    .onFailure { LOG.warn("Failed to shutdown MCP clients for project ${project.name}", it) }
            }
        }
    }
}
