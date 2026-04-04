package com.miracle.ui.settings.skills

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.config.AgentSettings
import com.miracle.services.AgentService
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.components.CardListComponents
import com.miracle.ui.settings.components.CardListComponents.createCardDescription
import com.miracle.ui.settings.components.CardListComponents.createCardSectionHeader
import com.miracle.ui.settings.components.CardListComponents.createCardListScrollPane
import com.miracle.ui.settings.components.CardListComponents.createCardHeaderRow
import com.miracle.ui.settings.components.CardListComponents.createCardBody
import com.miracle.ui.settings.components.CardListComponents.createCardShell
import com.miracle.ui.settings.components.CardListComponents.createIconActionButton
import com.miracle.ui.settings.components.CardListComponents.renderCardList
import com.miracle.ui.settings.skills.SkillDialogActions.showAddSkillDialog
import com.miracle.utils.SkillConfig
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.BoxLayout
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants


/**
 * Skills 管理面板的主 UI 组件
 */
@Suppress("DialogTitleCapitalization")
class SkillsPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()), Disposable {

    private val agentService = project.service<AgentService>()

    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }
    private val refreshAlarm: Alarm
    private val refreshTicket = AtomicLong(0)

    init {
        Disposer.register(parentDisposable, this)
        refreshAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
        isOpaque = false
        border = JBUI.Borders.empty(12)
        add(
            createCardSectionHeader(
                title = "Skills",
                subtitle = "Repeatable, customizable instructions you can reuse in any chat.",
                buttonText = "Add skill",
                buttonIcon = AllIcons.General.Add,
            ) { showAddSkillDialog(project, agentService) { refresh() } },
            BorderLayout.NORTH,
        )
        add(createCardListScrollPane(listPanel), BorderLayout.CENTER)
        agentService.skillLoader.addChangeListener(this) { scheduleRefresh(REFRESH_DEBOUNCE_MS) }
        refresh()
    }

    fun refresh() {
        scheduleRefresh(0)
    }

    private fun scheduleRefresh(delayMs: Int) {
        if (refreshAlarm.isDisposed) return
        val requestId = refreshTicket.incrementAndGet()
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest({
            val skills = runCatching { agentService.skillLoader.loadAllSkills(false).allSkills }
                .getOrDefault(emptyList())
            ApplicationManager.getApplication().invokeLater({
                if (refreshAlarm.isDisposed || project.isDisposed) return@invokeLater
                if (requestId == refreshTicket.get()) {
                    renderSkills(skills)
                }
            }, ModalityState.any())
        }, delayMs)
    }

    private fun renderSkills(skills: List<SkillConfig>) {
        val cards = skills
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
            .map { createSkillCard(it) }
        renderCardList(listPanel, cards, "No skills yet. Click Add skill to ask AI to draft one for you.")
    }

    private fun createSkillCard(skill: SkillConfig): JComponent {
        val disabled = isSkillDisabled(skill.name)
        val icon = McpUiComponents.LetterIcon(skill.name, JBUI.scale(32))

        val title = JBLabel(skill.name).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
            foreground = if (disabled) JBColor.GRAY else JBColor.foreground()
        }

        val meta = JBLabel(skillMeta(skill)).apply {
            font = JBFont.small()
            foreground = if (disabled) JBColor.GRAY else JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        val toggle = McpUiComponents.createServerToggle(!disabled) { enabled ->
            setSkillEnabled(skill.name, enabled)
        }

        val eastPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(createIconActionButton(AllIcons.Actions.GC, "Delete skill") { deleteSkill(skill) })
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(createIconActionButton(AllIcons.Actions.Find, "Open SKILL.md") { openSkillFile(skill) })
            add(Box.createHorizontalStrut(JBUI.scale(6)))
            add(toggle)
        }

        val header = createCardHeaderRow(icon, title, meta, eastPanel)
        val body = createCardBody(createCardDescription(skill.description, disabled))
        return createCardShell(header, body, CARD_HEIGHT, disabled)
    }

    private fun skillMeta(skill: SkillConfig): String = when (skill.scope) {
        SkillConfig.Scope.USER -> "User"
        SkillConfig.Scope.PROJECT -> "Project"
    }

    private fun isSkillDisabled(name: String): Boolean {
        return AgentSettings.state.disabledSkills.contains(name)
    }

    private fun setSkillEnabled(name: String, enabled: Boolean) {
        val settings = ApplicationManager.getApplication().getService(AgentSettings::class.java)
        val disabled = settings.state.disabledSkills
        if (enabled) {
            disabled.remove(name)
        } else if (!disabled.contains(name)) {
            disabled.add(name)
        }
        agentService.skillLoader.clearCache()
        refresh()
    }

    private fun openSkillFile(skill: SkillConfig) {
        val vf = LocalFileSystem.getInstance().findFileByPath(skill.filePath)
        if (vf == null) {
            Messages.showErrorDialog(project, "无法找到文件：${skill.filePath}", "打开 SKILL.md 失败")
            return
        }
        FileEditorManager.getInstance(project).openFile(vf, true)
        ProjectView.getInstance(project).select(vf, vf, true)
    }

    private fun deleteSkill(skill: SkillConfig) {
        val confirmed = Messages.showOkCancelDialog(
            project,
            "确定要删除 Skill \"${skill.name}\" 吗？\n将删除整个 Skill 目录及其所有文件，此操作不可撤销。",
            "Delete Skill",
            "Delete",
            "Cancel",
            Messages.getQuestionIcon(),
        )
        if (confirmed != Messages.OK) return

        try {
            val skillDir = java.io.File(skill.filePath).parentFile
            if (skillDir != null && skillDir.exists()) {
                skillDir.deleteRecursively()
                LocalFileSystem.getInstance().refresh(false)
            }
            agentService.skillLoader.clearCache()
            refresh()
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "删除 Skill 失败：${e.message}", "删除失败")
        }
    }

    override fun dispose() = Unit

    companion object {
        private const val CARD_HEIGHT = 160
        private const val REFRESH_DEBOUNCE_MS = 200
    }
}
