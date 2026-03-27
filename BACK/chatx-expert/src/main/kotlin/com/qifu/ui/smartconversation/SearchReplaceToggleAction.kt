package com.qifu.ui.smartconversation

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.JBColor
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.settings.configuration.ChatMode
import com.qifu.utils.getProjectConfigDirectory
import java.awt.Color
import java.nio.file.Paths
import javax.swing.JComponent

class SearchReplaceToggleAction(
    private val userInputPanel: UserInputPanel
) : ComboBoxAction() {

    init {
        isSmallVariant = true
        updateTemplatePresentation()
    }

    fun createCustomComponent(place: String): JComponent {
        return createCustomComponent(templatePresentation, place)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val currentMode = getCurrentMode()
        e.presentation.description = buildDynamicTooltip(currentMode)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = createComboBoxButton(presentation)
        button.foreground = JBColor.lazy {
            EditorColorsManager.getInstance().globalScheme.defaultForeground
        }
        button.border = null
        // 让下拉按钮背景透明
        button.putClientProperty("JButton.backgroundColor", Color(0, 0, 0, 0))
        return button
    }

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        ChatMode.entries.forEach { mode ->
            actionGroup.add(createModeAction(mode))
        }
//        actionGroup.addSeparator()
//        // 用户级
//        val customAgentList = getCustomAgentList()
//        customAgentList.forEach { customAgent ->
//            actionGroup.add(createModeAction(customAgent))
//        }
        return actionGroup
    }

    private fun createModeAction(mode: ChatMode): AnAction {
        return object : DumbAwareAction(mode.displayName, mode.description, null) {
            override fun update(event: AnActionEvent) {
                val presentation = event.presentation
                presentation.isEnabledAndVisible = true

//                val currentMode = userInputPanel.getChatMode()
//
//                if (mode == currentMode) {
//                    presentation.isEnabled = false
//                } else if (!mode.isEnabled) {
//                    presentation.isEnabled = true
//                    presentation.putClientProperty("ActionButton.noBackground", false)
//                }
            }

            override fun actionPerformed(e: AnActionEvent) {
//                if (!mode.isEnabled) return

                userInputPanel.setChatMode(mode)
                updateTemplatePresentation()
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }
        }
    }

    private fun updateTemplatePresentation() {
        val currentMode = getCurrentMode()
        templatePresentation.text = currentMode.displayName
        templatePresentation.description = buildDynamicTooltip(currentMode)
    }

    fun refreshPresentation() {
        updateTemplatePresentation()
    }

    private fun getCurrentMode(): ChatMode = userInputPanel.getChatMode()

    private fun buildDynamicTooltip(currentMode: ChatMode): String {
        return """
            <html>
            <head></head>
            <body>
                <div class="content">
                    <div class="bottom">
                        <b>${currentMode.displayName} Mode</b>
                    </div>
                    <div style="margin-top: 8px; color:#bcbec4;">
                        ${getModeDescription(currentMode)}
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getModeDescription(mode: ChatMode): String {
        return when (mode) {
            ChatMode.ASK ->
                "快速解惑，智能答码"

            ChatMode.AGENT ->
                "规划、查找、构建所需的一切"

            ChatMode.PLAN ->
                "只读探索与方案设计"

            else -> {""}
        }
    }

    private fun getCustomAgentList(): List<ChatMode> {
        // todo 从文件夹提取文件夹名
        val projectConfigDir = getProjectConfigDirectory()
        val projectAgentDir = Paths.get(projectConfigDir, "agents")
        val dir = projectAgentDir.toFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val mdFileNames = dir.listFiles { f -> f.extension == "md" }?.map { it.nameWithoutExtension } ?: emptyList()

        return mdFileNames.map { ChatMode(it, "Agent Config$it") }

//        return listOf(
//            ChatMode("Test", "test"),
//            ChatMode("CLASS", "class"),
//        )
    }
}
