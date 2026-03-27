package com.qifu.ui.smartconversation

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.qifu.config.AgentSettings
import com.qifu.services.AgentService
import com.qifu.ui.settings.skills.SkillDialogActions
import com.qifu.utils.SkillConfig
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class SkillsSelectionComponent(
    private val project: Project,
) {
    private val agentService = project.service<AgentService>()
    private var availableSkills: List<SkillConfig> = emptyList()
    private var popup: JBPopup? = null
    private val maxPopupHeight = JBUI.scale(220)
    private val maxPopupWidth = JBUI.scale(300)
    private val button = JButton().apply {
        isOpaque = false
        isContentAreaFilled = false
        border = JBUI.Borders.empty()
        horizontalAlignment = SwingConstants.LEFT
        font = JBFont.small()
        addActionListener { showPopup() }
    }

    init {
        updatePresentation()
    }

    fun getComponent(): JComponent = button

    private fun showPopup() {
        if (popup?.isDisposed == false && popup?.isVisible == true) {
            popup?.cancel()
            return
        }
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
        }
        populateSkillPanel(panel)
        val scrollPane = ScrollPaneFactory.createScrollPane(panel, true).apply {
            border = JBUI.Borders.empty()
            viewportBorder = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

            val preferred = panel.preferredSize
            val preferredWidth = preferred.width.coerceAtMost(maxPopupWidth)
            val preferredHeight = preferred.height.coerceAtMost(maxPopupHeight)
            preferredSize = Dimension(preferredWidth, preferredHeight)
            maximumSize = Dimension(maxPopupWidth, maxPopupHeight)
        }
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, panel)
            .setRequestFocus(true)
            .setResizable(true)
            .setMovable(false)
            .setCancelOnClickOutside(true)
            .createPopup()

        val source: JComponent = button
        val point = RelativePoint.getSouthWestOf(source)
        popup?.show(point)
    }

    private fun populateSkillPanel(container: JPanel) {
        refreshAvailableSkills()
        container.removeAll()
        if (availableSkills.isEmpty()) {
            container.add(JBLabel("No skills available").apply {
                foreground = JBColor.GRAY
                alignmentX = Component.LEFT_ALIGNMENT
            })
        } else {
            val disabled = AgentSettings.state.disabledSkills
            availableSkills.forEach { skill ->
                val checkbox = JCheckBox(skill.name, !disabled.contains(skill.name)).apply {
                    isOpaque = false
                    toolTipText = buildTooltipDescription(skill.description)
                    alignmentX = Component.LEFT_ALIGNMENT
                    addActionListener {
                        toggleSkill(skill.name, isSelected)
                        updatePresentation()
                    }
                }

                val descText = skill.description.takeIf { it.isNotBlank() }?.let { truncateDescription(it) }
                val descriptionLabel = descText?.let {
                    JBLabel(it).apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        foreground = JBColor.GRAY
                        font = JBUI.Fonts.miniFont()
                        border = JBUI.Borders.emptyLeft(2)
                    }
                }

                val row = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    alignmentX = Component.LEFT_ALIGNMENT
                    border = JBUI.Borders.emptyBottom(3)
                    isOpaque = true
                    background = UIUtil.getListBackground()
                }
                row.add(checkbox)
                if (descriptionLabel != null) {
                    row.add(Box.createVerticalStrut(JBUI.scale(2)))
                    row.add(descriptionLabel)
                }
                row.maximumSize = Dimension(Int.MAX_VALUE, row.preferredSize.height)
                installHoverHighlight(row, checkbox, descriptionLabel)

                container.add(row)
                container.add(Box.createVerticalStrut(JBUI.scale(4)))
            }
        }

        container.add(Box.createVerticalStrut(JBUI.scale(4)))
        container.add(JButton("Add skill", AllIcons.General.Add).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                SkillDialogActions.showAddSkillDialog(project, agentService) {
                    populateSkillPanel(container)
                    SwingUtilities.invokeLater { container.revalidate(); container.repaint() }
                }
            }
        })

        updatePresentation()
        container.revalidate()
        container.repaint()
    }

    private fun loadSkills(): List<SkillConfig> {
        return runCatching { agentService.skillLoader.loadAllSkills(false).allSkills }.getOrDefault(emptyList())
    }

    private fun refreshAvailableSkills() {
        availableSkills = loadSkills()
    }

    private fun toggleSkill(name: String, enabled: Boolean) {
        val disabled = AgentSettings.state.disabledSkills
        if (enabled) {
            disabled.remove(name)
        } else if (!disabled.contains(name)) {
            disabled.add(name)
        }
        agentService.skillLoader.clearCache()
    }

    private fun installHoverHighlight(container: JComponent, vararg related: JComponent?) {
        val normalBg = UIUtil.getListBackground()
        val hoverBg = UIUtil.getListSelectionBackground(true)
        val listener = object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                container.background = hoverBg
            }

            override fun mouseExited(e: MouseEvent?) {
                e ?: return
                val source = e.component
                val exitPoint = SwingUtilities.convertPoint(source, e.point, container)
                if (container.contains(exitPoint)) return
                container.background = normalBg
            }
        }
        (listOf(container) + related.filterNotNull()).forEach { comp ->
            comp.addMouseListener(listener)
        }
    }

    private fun truncateDescription(desc: String, maxLength: Int = 80): String {
        if (desc.length <= maxLength) return desc
        return desc.take(maxLength - 3) + "..."
    }

    private fun buildTooltipDescription(desc: String?): String? {
        return desc?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun updatePresentation() {
        refreshAvailableSkills()
        val total = availableSkills.size
        button.icon = Icons.SKILLS
        if (total <= 0) {
            button.text = "Skills"
            button.toolTipText = "当前没有可用的技能"
            return
        }
        val disabled = AgentSettings.state.disabledSkills.toSet()
        val enabledCount = (total - disabled.count { id -> availableSkills.any { it.name == id } }).coerceAtLeast(0)
        button.text = if (disabled.isEmpty()) {
            "Skills($enabledCount)"
        } else {
            "Skills ($enabledCount/$total)"
        }
        button.toolTipText = "选择当前会话可用的技能"
    }
}
