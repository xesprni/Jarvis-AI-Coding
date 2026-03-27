package com.miracle.ui.settings.mcp.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.util.addMouseHoverListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.hover.HoverListener
import com.intellij.util.ui.JBUI
import com.miracle.agent.mcp.McpConfigManager
import com.miracle.agent.mcp.McpInstallScope
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

/**
 * MCP 配置文件操作的底部面板
 *
 * 包含打开项目级和全局 MCP 配置文件的按钮
 */
@Suppress("UnstableApiUsage")
class McpConfigFooterPanel(private val project: Project) : JPanel(BorderLayout()) {

    init {
        isOpaque = false
        border = JBUI.Borders.emptyTop(12)

        // 添加顶部分隔线
        add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.NORTH)

        // 添加按钮行
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyTop(12)

            add(createConfigButton("打开项目 MCP 配置") {
                openConfig(McpInstallScope.PROJECT)
            })
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(createConfigButton("打开全局 MCP 配置") {
                openConfig(McpInstallScope.GLOBAL)
            })
            add(Box.createHorizontalGlue())
        }, BorderLayout.CENTER)
    }

    private fun openConfig(scope: McpInstallScope) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val error = McpConfigManager.openConfigFile(project, scope)
            if (error != null) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "打开 MCP 配置失败：$error",
                        "打开 MCP 配置失败"
                    )
                }
            }
        }
    }

    private fun createConfigButton(text: String, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            val defaultBorder = JBUI.Borders.empty(1)
            border = defaultBorder
            addActionListener { onClick() }

            addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    border = JBUI.Borders.customLine(HIGHLIGHT_COLOR, 1)
                    repaint()
                }
                override fun focusLost(e: FocusEvent?) {
                    border = defaultBorder
                    repaint()
                }
            })

            addMouseHoverListener(null, object : HoverListener() {
                override fun mouseEntered(component: Component, x: Int, y: Int) {
                    if (!isFocusOwner) {
                        border = JBUI.Borders.customLine(HIGHLIGHT_COLOR, 1)
                        repaint()
                    }
                }
                override fun mouseExited(component: Component) {
                    if (!isFocusOwner) {
                        border = defaultBorder
                        repaint()
                    }
                }
                override fun mouseMoved(component: Component, x: Int, y: Int) {}
            })
        }
    }

    companion object {
        private val HIGHLIGHT_COLOR = JBColor(Color(0x47, 0x8B, 0xFF), Color(0x57, 0x6D, 0xC8))
    }
}
