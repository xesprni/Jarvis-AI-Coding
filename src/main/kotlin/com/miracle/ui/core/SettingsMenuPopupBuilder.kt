package com.miracle.ui.core

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.Component

/**
 * 构建并显示 Jarvis 设置菜单弹窗，列出所有设置分区供用户选择。
 *
 * @param project 当前项目实例
 */
internal class SettingsMenuPopupBuilder(
    private val project: Project,
) {
    /**
     * 在指定锚点组件下方显示设置菜单弹窗。
     *
     * @param anchor 弹窗锚点组件
     */
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
