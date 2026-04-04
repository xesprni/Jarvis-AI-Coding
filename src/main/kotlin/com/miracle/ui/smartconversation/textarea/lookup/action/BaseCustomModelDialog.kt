package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelApiStyle
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JPasswordField

/**
 * 添加/编辑自定义模型对话框的共享基类。
 *
 * 封装了表单字段创建、GridBagLayout 布局、校验骨架和结果收集逻辑。
 * 子类只需提供初始值和差异化校验规则。
 */
abstract class BaseCustomModelDialog(
    project: Project?,
    private val apiKeyLabel: String,
) : DialogWrapper(project) {

    // ── 表单字段 ──────────────────────────────────────────────────────

    protected val modelNameField = JBTextField()
    protected val aliasField = JBTextField()
    protected val endpointField = JBTextField()
    protected val apiKeyField = JPasswordField()
    protected val contextTokensField = JBTextField("128000")
    protected val apiStyleComboBox = JComboBox(ModelApiStyle.entries.toTypedArray())
    protected val reasoningEffortComboBox = JComboBox(REASONING_OPTIONS)
    protected val supportsImagesCheckBox = JBCheckBox("支持多模态(图片)", false)

    init {
        arrayOf(modelNameField, aliasField, endpointField, apiKeyField).forEach {
            it.preferredSize = Dimension(JBUI.scale(400), it.preferredSize.height)
        }
        contextTokensField.preferredSize = Dimension(JBUI.scale(150), contextTokensField.preferredSize.height)
        apiStyleComboBox.preferredSize = Dimension(JBUI.scale(180), apiStyleComboBox.preferredSize.height)
        reasoningEffortComboBox.preferredSize = Dimension(JBUI.scale(180), reasoningEffortComboBox.preferredSize.height)

        apiStyleComboBox.addActionListener { updateCredentialFieldState() }
        updateCredentialFieldState()
    }

    // ── 布局 ─────────────────────────────────────────────────────────

    override fun createCenterPanel(): JComponent {
        val form = JPanel(GridBagLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
        }

        val c = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        addRow(form, c, 0, "模型名称:", modelNameField)
        addRow(form, c, 1, "显示名称:", aliasField)
        addRow(form, c, 2, "API 地址:", endpointField)
        addRow(form, c, 3, apiKeyLabel, apiKeyField)
        addRow(form, c, 4, "上下文长度:", contextTokensField)
        addRow(form, c, 5, "接口类型:", apiStyleComboBox)
        addRow(form, c, 6, "Reasoning:", reasoningEffortComboBox)
        addRow(form, c, 7, "", supportsImagesCheckBox)

        return form
    }

    private fun addRow(panel: JPanel, c: GridBagConstraints, row: Int, label: String, field: JComponent) {
        addRow(panel, c, row, JBLabel(label), field)
    }

    private fun addRow(panel: JPanel, c: GridBagConstraints, row: Int, label: JComponent, field: JComponent) {
        val labelConstraints = c.clone() as GridBagConstraints
        labelConstraints.gridx = 0
        labelConstraints.gridy = row
        labelConstraints.weightx = 0.0
        labelConstraints.anchor = GridBagConstraints.WEST
        panel.add(label, labelConstraints)

        val fieldConstraints = c.clone() as GridBagConstraints
        fieldConstraints.gridx = 1
        fieldConstraints.gridy = row
        fieldConstraints.weightx = 1.0
        panel.add(field, fieldConstraints)
    }

    // ── 校验骨架 ──────────────────────────────────────────────────────

    final override fun doValidate(): ValidationInfo? {
        val modelName = modelNameField.text.trim()
        if (modelName.isBlank()) {
            return ValidationInfo("请输入模型名称", modelNameField)
        }
        validateModelName(modelName)?.let { return it }
        if (endpointField.text.trim().isBlank()) {
            return ValidationInfo("请输入 API 地址", endpointField)
        }
        validateApiKey()?.let { return it }
        val contextTokens = contextTokensField.text.toIntOrNull()
        if (contextTokens == null || contextTokens <= 0) {
            return ValidationInfo("上下文长度必须是大于 0 的整数", contextTokensField)
        }
        return null
    }

    /** 子类可 override 以添加模型名称的唯一性校验 */
    protected open fun validateModelName(modelName: String): ValidationInfo? = null

    /** 子类可 override 以定制 API Key 校验（如编辑模式允许为空） */
    protected open fun validateApiKey(): ValidationInfo? = null

    // ── 结果收集 ──────────────────────────────────────────────────────

    fun getModelInfo(): ModelInfo {
        return ModelInfo(
            model = modelNameField.text.trim(),
            endpoint = endpointField.text.trim(),
            apiKey = String(apiKeyField.password).trim(),
            contextTokens = contextTokensField.text.toInt(),
            alias = aliasField.text.trim(),
            apiStyle = apiStyleComboBox.selectedItem as? ModelApiStyle ?: ModelApiStyle.CHAT_COMPLETIONS,
            reasoningEffort = parseReasoningEffort(reasoningEffortComboBox.selectedItem as? String),
            supportsImages = supportsImagesCheckBox.isSelected
        )
    }

    // ── 工具方法 ──────────────────────────────────────────────────────

    private fun updateCredentialFieldState() {
        endpointField.isEnabled = true
        apiKeyField.isEnabled = true
    }

    // ── 公共数据类与常量 ──────────────────────────────────────────────

    data class ModelInfo(
        val model: String,
        val endpoint: String,
        val apiKey: String,
        val contextTokens: Int,
        val alias: String,
        val apiStyle: ModelApiStyle = ModelApiStyle.CHAT_COMPLETIONS,
        val reasoningEffort: String? = null,
        val supportsImages: Boolean = false
    )

    companion object {
        val REASONING_OPTIONS = arrayOf("默认", "low", "medium", "high", "xhigh")

        fun parseReasoningEffort(value: String?): String? {
            val trimmed = value?.trim()
            return trimmed?.takeIf { it.isNotBlank() && it != "默认" }
        }
    }
}
