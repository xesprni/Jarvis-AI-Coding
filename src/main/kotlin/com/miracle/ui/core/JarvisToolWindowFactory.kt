package com.miracle.ui.core

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.Icon

/**
 * Jarvis 工具窗口工厂，负责创建和注册 Jarvis 工具窗口的内容面板。
 */
class JarvisToolWindowFactory : ToolWindowFactory, DumbAware {
    /**
     * 创建工具窗口内容，初始化面板并绑定到服务。
     *
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setIcon(TOOL_WINDOW_ICON)
        val panel = JarvisToolWindowPanel(project, toolWindow.disposable)
        project.getService(JarvisToolWindowService::class.java).bind(panel)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.setDisposer(panel)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Jarvis"
        private val TOOL_WINDOW_ICON: Icon =
            IconLoader.getIcon("/img/inner/logo_round13.svg", JarvisToolWindowFactory::class.java)
    }
}
