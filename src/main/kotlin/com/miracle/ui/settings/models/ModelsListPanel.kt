package com.miracle.ui.settings.models

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelConfig
import com.miracle.services.updateCustomModel
import com.miracle.services.addCustomModel
import com.miracle.services.deleteCustomModel
import com.miracle.services.formatReasoningEffort
import com.miracle.services.getCustomModels
import com.miracle.ui.settings.mcp.components.McpUiComponents
import com.miracle.ui.settings.components.CardListComponents.createCardSectionHeader
import com.miracle.ui.settings.components.CardListComponents.createCardListScrollPane
import com.miracle.ui.settings.components.CardListComponents.createCardHeaderRow
import com.miracle.ui.settings.components.CardListComponents.createCardBody
import com.miracle.ui.settings.components.CardListComponents.createCardShell
import com.miracle.ui.settings.components.CardListComponents.createIconActionButton
import com.miracle.ui.settings.components.CardListComponents.renderCardList
import com.miracle.ui.smartconversation.textarea.lookup.action.AddCustomModelDialog
import com.miracle.ui.smartconversation.textarea.lookup.action.EditCustomModelDialog
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * 模型管理面板 — 采用与 SkillsPanel 一致的卡片列表风格。
 */
@Suppress("DialogTitleCapitalization")
class ModelsListPanel(
    private val project: Project,
    private val onModelsChanged: (() -> Unit)? = null,
) : JPanel(BorderLayout()) {

    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(12)
        add(
            createCardSectionHeader(
                title = "模型",
                subtitle = "管理本地自定义模型，所有模型来自 models.json 配置文件。",
                buttonText = "添加模型",
                buttonIcon = AllIcons.General.Add,
            ) { showAddDialog() },
            BorderLayout.NORTH,
        )
        add(createCardListScrollPane(listPanel), BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        val models = getCustomModels()
        renderModels(models)
    }

    private fun renderModels(models: List<ModelConfig>) {
        val cards = models
            .sortedBy { it.alias.lowercase() }
            .map { createModelCard(it) }
        renderCardList(listPanel, cards, "暂无模型配置。点击「添加模型」以添加自定义模型。")
    }

    private fun createModelCard(model: ModelConfig): JComponent {
        val icon = McpUiComponents.LetterIcon(model.alias, JBUI.scale(32))

        val titleLabel = JBLabel(model.alias).apply {
            font = JBFont.label().asBold().biggerOn(0.5f)
        }

        val meta = JBLabel(modelMeta(model)).apply {
            font = JBFont.small()
            foreground = JBColor(Color(110, 118, 132), Color(150, 160, 175))
        }

        val eastPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(createIconActionButton(AllIcons.Actions.Edit, "编辑模型") { showEditDialog(model) })
            add(Box.createHorizontalStrut(JBUI.scale(6)))
            add(createIconActionButton(AllIcons.Actions.GC, "删除模型") { confirmDelete(model) })
        }

        val descText = buildString {
            append("Endpoint: ${model.endpoint}")
            append("  |  ${model.resolvedApiStyle.desc}")
            append("  |  Reasoning ${formatReasoningEffort(model.resolvedReasoningEffort)}")
            if (model.supportsImages) append("  |  多模态")
        }
        val desc = JBLabel(descText).apply {
            font = JBFont.label()
            foreground = JBColor(Color(50, 60, 80), Color(200, 205, 215))
        }

        val header = createCardHeaderRow(icon, titleLabel, meta, eastPanel)
        val body = createCardBody(desc)
        return createCardShell(header, body, CARD_HEIGHT)
    }

    private fun modelMeta(model: ModelConfig): String {
        val ctx = when {
            model.contextTokens >= 1000 -> "${model.contextTokens / 1000}K"
            else -> "${model.contextTokens}"
        }
        return "${model.model}  ·  上下文 $ctx tokens"
    }

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
                    apiStyle = info.apiStyle,
                    reasoningEffort = info.reasoningEffort,
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
                    apiStyle = info.apiStyle,
                    reasoningEffort = info.reasoningEffort,
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
    }
}
