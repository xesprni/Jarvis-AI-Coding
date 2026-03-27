package com.miracle.ui.core

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.Icon

class JarvisToolWindowFactory : ToolWindowFactory, DumbAware {
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
