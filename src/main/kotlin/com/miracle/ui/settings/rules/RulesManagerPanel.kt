package com.miracle.ui.settings.rules

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

class RulesManagerPanel(private val project: com.intellij.openapi.project.Project) : JPanel(BorderLayout()) {

    init {
        border = JBUI.Borders.empty(16)
        isOpaque = false
        val description = JTextArea(
            "项目规则文件使用 AGENTS.md 管理。创建或打开后，你可以直接在编辑器中维护本地规则模板。"
        ).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty()
            background = JBColor.PanelBackground
        }
        add(JBLabel("Rules").apply {
            font = font.deriveFont(font.size2D + 2f).deriveFont(java.awt.Font.BOLD)
            border = JBUI.Borders.emptyBottom(12)
        }, BorderLayout.NORTH)
        add(description, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(JButton("打开项目 AGENTS.md").apply {
                addActionListener { RulesComponent.openRulesConfig(project) }
            })
        }, BorderLayout.SOUTH)
    }
}
