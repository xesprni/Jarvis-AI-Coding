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
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.config.AgentSettings
import com.miracle.services.AgentService
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.skills.SkillDialogActions.showAddSkillDialog
import com.miracle.utils.SkillConfig
import java.awt.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.swing.*


/**
 * Skills 管理面板的主 UI 组件
 * 
 */
@Suppress("DialogTitleCapitalization")
class SkillsPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()), Disposable {

    /** Agent 服务实例，用于加载和管理Skill */
    private val agentService = project.service<AgentService>()

    /** Skill列表容器，使用垂直布局显示所有Skill卡片 */
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
        add(createHeader(), BorderLayout.NORTH)
        add(createListContainer(), BorderLayout.CENTER)
        agentService.skillLoader.addChangeListener(this) { scheduleRefresh(REFRESH_DEBOUNCE_MS) }
        refresh()
    }

    fun refresh() {
        scheduleRefresh(0)
    }

    /**
     * 延迟刷新Skill列表
     * 
     * 使用防抖机制避免短时间内多次触发刷新。
     * 每次调用会取消之前的待执行刷新任务，重新调度一个新任务。
     *
     * 通过 requestId 机制确保只有最新的刷新请求会更新 UI，
     * 避免过时的异步结果覆盖最新状态。
     * 
     */
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

    /**
     * 创建面板顶部的标题区域
     * 
     */
    private fun createHeader(): JComponent {
        val title = JBLabel("Skills", SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }
        val subtitle = JBLabel("Repeatable, customizable instructions you can reuse in any chat.").apply {
            foreground = JBColor(Color(80, 90, 110), Color(170, 180, 195))
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val addButton = JButton("Add skill", AllIcons.General.Add).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener {
                showAddSkillDialog(project, agentService) { refresh() }
            }
            maximumSize = preferredSize
        }

        val topRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(title)
            add(Box.createHorizontalGlue())
            add(addButton)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyBottom(8)
            add(topRow)
            add(subtitle)
        }
    }

    /**
     * 创建Skill列表的滚动容器
     * 
     */
    private fun createListContainer(): JComponent {
        val scrollPane = JBScrollPane(listPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }
        return scrollPane
    }

    /**
     * 渲染Skill列表
     * 
     */
    private fun renderSkills(skills: List<SkillConfig>) {
        listPanel.removeAll()
        if (skills.isEmpty()) {
            listPanel.add(createEmptyState())
        } else {
            // 按名称排序以保证显示顺序稳定，并在卡片之间渲染分隔线
            skills.sortedBy { it.name.lowercase(Locale.getDefault()) }
                .forEachIndexed { index, skill ->
                    if (index != 0) {
                        listPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                            maximumSize = Dimension(Int.MAX_VALUE, 10)
                        })
                    }
                    listPanel.add(createSkillCard(skill).apply { alignmentX = Component.LEFT_ALIGNMENT })
                }
            listPanel.add(Box.createVerticalStrut(JBUI.scale(12)))
        }
        listPanel.revalidate()
        listPanel.repaint()
    }

    /**
     * 为单个Skill创建卡片组件
     * 
     */
    private fun createSkillCard(skill: SkillConfig): JComponent {
        val disabled = isSkillDisabled(skill.name)
        val icon = McpUiComponents.LetterIcon(skill.name, JBUI.scale(32))

        val title = JBLabel(skill.name).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
            foreground = if (disabled) JBColor.GRAY else JBColor.foreground()
        }

        val desc = createDescriptionComponent(skill.description, disabled)

        val meta = JBLabel(skillMeta(skill)).apply {
            font = JBFont.small()
            foreground = if (disabled) JBColor.GRAY else JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        // 启用/禁用切换开关
        val toggle = McpUiComponents.createServerToggle(!disabled) { enabled ->
            setSkillEnabled(skill.name, enabled)
        }

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel(icon).apply {
                preferredSize = Dimension(JBUI.scale(40), JBUI.scale(40))
                horizontalAlignment = SwingConstants.CENTER
                verticalAlignment = SwingConstants.CENTER
            }, BorderLayout.WEST)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(title)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(meta)
            }, BorderLayout.CENTER)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false
                add(createLocateButton(skill))
                add(Box.createHorizontalStrut(JBUI.scale(6)))
                add(toggle)
            }, BorderLayout.EAST)
        }

        val body = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            add(desc, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = if (disabled) JBColor(Color(0xF7, 0xF8, 0xFA), Color(0x2B, 0x2D, 0x32)) else JBColor.PanelBackground
            border = JBUI.Borders.empty(12)
            minimumSize = Dimension(0, JBUI.scale(CARD_HEIGHT))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    /**
     * 生成Skill的元信息文本
     */
    private fun skillMeta(skill: SkillConfig): String {
        val scope = when (skill.scope) {
            SkillConfig.Scope.USER -> "User"
            SkillConfig.Scope.PROJECT -> "Project"
        }
        return scope
    }

    /**
     * 创建Skill描述文本组件
     * 
     * 描述文本会自动换行以适应容器宽度，超过 260 字符时会截断并添加省略号。
     * 
     */
    private fun createDescriptionComponent(description: String, disabled: Boolean): JComponent {
        val normalized = description.trim()
        val shortened = if (normalized.length > 260) normalized.take(260) + "..." else normalized
        // 文本区域自动换行以适应容器宽度，避免出现横向滚动条
        return JBTextArea(shortened).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            font = JBFont.label()
            foreground = if (disabled) JBColor(Color(140, 145, 155), Color(120, 125, 135))
            else JBColor(Color(50, 60, 80), Color(200, 205, 215))
            border = null
            minimumSize = Dimension(0, preferredSize.height)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    /**
     * 创建空状态提示组件
     * 
     */
    private fun createEmptyState(): JComponent {
        val label = JBLabel("No skills yet. Click Add skill to ask AI to draft one for you.").apply {
            foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            add(label, BorderLayout.WEST)
        }
    }

    /**
     * 检查指定Skill是否已被禁用
     * 
     */
    private fun isSkillDisabled(name: String): Boolean {
        return AgentSettings.state.disabledSkills.contains(name)
    }

    /**
     * 设置Skill的启用/禁用状态
     * 
     * 更新配置并清除Skill加载器缓存，然后刷新界面。
     * 
     */
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

    companion object {
        /** Skill卡片的固定高度 */
        private const val CARD_HEIGHT = 160
        private const val REFRESH_DEBOUNCE_MS = 200
    }

    override fun dispose() {
        // Alarm is a child disposable of this panel and will be disposed automatically.
        // No additional cleanup needed.
    }

    private fun createLocateButton(skill: SkillConfig): JButton {
        return JButton(AllIcons.Actions.Find).apply {
            toolTipText = "Open SKILL.md"
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { openSkillFile(skill) }
            preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
            maximumSize = preferredSize
        }
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
}
