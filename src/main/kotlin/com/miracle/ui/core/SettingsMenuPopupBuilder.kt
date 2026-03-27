package com.miracle.ui.core

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.Component

internal class SettingsMenuPopupBuilder(
    private val project: Project,
) {
    fun show(anchor: Component) {
        val actionGroup: ActionGroup = DefaultActionGroup().apply {
            JarvisSettingsSection.entries.forEach { section ->
                add(object : DumbAwareAction(section.title, section.description, section.icon) {
                    override fun actionPerformed(e: AnActionEvent) {
                        project.getService(JarvisToolWindowService::class.java).showSettings(section)
                    }
                })
            }
        }

        JBPopupFactory.getInstance().createActionGroupPopup(
            "Jarvis 设置",
            actionGroup,
            DataManager.getInstance().getDataContext(anchor),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
        ).showUnderneathOf(anchor)
    }
}
