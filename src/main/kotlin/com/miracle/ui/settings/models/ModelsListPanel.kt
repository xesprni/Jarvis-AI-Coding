package com.miracle.ui.settings.models

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelConfig
import com.miracle.services.deleteCustomModel
import com.miracle.services.getCustomModels
import com.miracle.services.addCustomModel
import com.miracle.services.updateCustomModel
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.smartconversation.textarea.lookup.action.AddCustomModelDialog
import com.miracle.ui.smartconversation.textarea.lookup.action.EditCustomModelDialog
import java.awt.*
import javax.swing.*

/**
 * 模型管理面板 — 采用与 SkillsPanel 一致的卡片列表风格。
 *
 * 每个模型以卡片形式展示，包含字母图标、模型名/别名、端点、
 * 上下文长度等元信息，以及编辑/删除操作按钮。
 */
@Suppress("DialogTitleCapitalization")
class ModelsListPanel(
    private val project: Project,
    private val onModelsChanged: (() -> Unit)? = null,
) : JPanel(BorderLayout()) {

    /** 模型卡片列表容器 */
    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(12)
        add(createHeader(), BorderLayout.NORTH)
        add(createListContainer(), BorderLayout.CENTER)
        refresh()
    }

    /** 重新从配置加载并渲染模型列表 */
    fun refresh() {
        val models = getCustomModels()
        renderModels(models)
    }

    // ───────── Header ─────────

    private fun createHeader(): JComponent {
        val title = JBLabel("模型", SwingConstants.LEFT).apply {
            font = JBFont.label().asBold().biggerOn(2f)
        }
        val subtitle = JBLabel("管理本地自定义模型，所有模型来自 models.json 配置文件。").apply {
            foreground = MUTED_FOREGROUND
            border = JBUI.Borders.emptyTop(4)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val addButton = JButton("添加模型", AllIcons.General.Add).apply {
            putClientProperty("JButton.backgroundColor", JBColor.PanelBackground)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { showAddDialog() }
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

    // ───────── Scroll container ─────────

    private fun createListContainer(): JComponent {
        return JBScrollPane(listPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(16)
        }
    }

    // ───────── Render list ─────────

    private fun renderModels(models: List<ModelConfig>) {
        listPanel.removeAll()
        if (models.isEmpty()) {
            listPanel.add(createEmptyState())
        } else {
            models.sortedBy { it.alias.lowercase() }
                .forEachIndexed { index, model ->
                    if (index != 0) {
                        listPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                            maximumSize = Dimension(Int.MAX_VALUE, 10)
                        })
                    }
                    listPanel.add(createModelCard(model).apply { alignmentX = Component.LEFT_ALIGNMENT })
                }
            listPanel.add(Box.createVerticalStrut(JBUI.scale(12)))
        }
        listPanel.revalidate()
        listPanel.repaint()
    }

    // ───────── Single card ─────────

    private fun createModelCard(model: ModelConfig): JComponent {
        val icon = McpUiComponents.LetterIcon(model.alias, JBUI.scale(32))

        val titleLabel = JBLabel(model.alias).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
        }

        val meta = JBLabel(modelMeta(model)).apply {
            font = JBFont.small()
            foreground = JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        val editButton = JButton(AllIcons.Actions.Edit).apply {
            toolTipText = "编辑模型"
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
            maximumSize = preferredSize
            addActionListener { showEditDialog(model) }
        }

        val deleteButton = JButton(AllIcons.Actions.GC).apply {
            toolTipText = "删除模型"
            isOpaque = false
            isContentAreaFilled = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
            maximumSize = preferredSize
            addActionListener { confirmDelete(model) }
        }

        // ── header row: icon | name+meta | buttons ──

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
                add(titleLabel)
                add(Box.createVerticalStrut(JBUI.scale(2)))
                add(meta)
            }, BorderLayout.CENTER)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isOpaque = false
                add(editButton)
                add(Box.createHorizontalStrut(JBUI.scale(6)))
                add(deleteButton)
            }, BorderLayout.EAST)
        }

        // ── description body ──

        val descText = buildString {
            append("Endpoint: ${model.endpoint}")
            if (model.supportsImages) append("  |  多模态")
        }

        val desc = JBLabel(descText).apply {
            font = JBFont.label()
            foreground = JBColor(Color(50, 60, 80), Color(200, 205, 215))
        }

        val body = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            add(desc, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = JBColor.PanelBackground
            border = JBUI.Borders.empty(12)
            minimumSize = Dimension(0, JBUI.scale(CARD_HEIGHT))
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(CARD_HEIGHT))
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    /** 模型卡片下方的元信息：模型名称 + 上下文长度 */
    private fun modelMeta(model: ModelConfig): String {
        val ctx = when {
            model.contextTokens >= 1000 -> "${model.contextTokens / 1000}K"
            else -> "${model.contextTokens}"
        }
        return "${model.model}  ·  上下文 $ctx tokens"
    }

    // ───────── Empty state ─────────

    private fun createEmptyState(): JComponent {
        val label = JBLabel("暂无模型配置。点击「添加模型」以添加自定义模型。").apply {
            foreground = JBColor(Color(90, 100, 120), Color(150, 160, 175))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(16)
            add(label, BorderLayout.WEST)
        }
    }

    // ───────── Add / Edit / Delete actions ─────────

    private fun showAddDialog() {
        val dialog = AddCustomModelDialog(project)
        if (dialog.showAndGet()) {
            val info = dialog.getModelInfo()
            try {
                addCustomModel(
                    model = info.model,
                    endpoint = info.endpoint,
                    apiKey = info.apiKey,
                    contextTokens = info.contextTokens,
                    alias = info.alias.ifBlank { null },
                    supportsImages = info.supportsImages,
                )
                onModelsChanged?.invoke()
                refresh()
            } catch (e: Exception) {
                Messages.showWarningDialog(project, e.message ?: "添加失败", "添加模型")
            }
        }
    }

    private fun showEditDialog(model: ModelConfig) {
        val dialog = EditCustomModelDialog(project, model)
        if (dialog.showAndGet()) {
            val info = dialog.getModelInfo()
            try {
                updateCustomModel(
                    oldModel = model.model,
                    newModel = info.model,
                    endpoint = info.endpoint,
                    apiKey = info.apiKey.ifBlank { null },
                    contextTokens = info.contextTokens,
                    alias = info.alias.ifBlank { null },
                    supportsImages = info.supportsImages,
                )
                onModelsChanged?.invoke()
                refresh()
            } catch (e: Exception) {
                Messages.showWarningDialog(project, e.message ?: "更新失败", "编辑模型")
            }
        }
    }

    private fun confirmDelete(model: ModelConfig) {
        val result = Messages.showYesNoDialog(
            project,
            "确定要删除模型「${model.alias}」吗？",
            "删除模型",
            Messages.getQuestionIcon(),
        )
        if (result == Messages.YES) {
            try {
                deleteCustomModel(model.model)
                onModelsChanged?.invoke()
                refresh()
            } catch (e: Exception) {
                Messages.showWarningDialog(project, e.message ?: "删除失败", "删除模型")
            }
        }
    }

    companion object {
        private const val CARD_HEIGHT = 120
        private val MUTED_FOREGROUND = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
    }
}
