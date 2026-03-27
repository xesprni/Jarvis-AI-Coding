package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelApiStyle
import com.miracle.services.isCustomModelExists
import com.miracle.services.requiresApiCredentials
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPasswordField

class AddCustomModelDialog(project: Project?) : DialogWrapper(project) {

    private val modelNameField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "例如: gpt-4.1"
    }

    private val aliasField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "可选, 留空则使用模型名称"
    }

    private val endpointField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        emptyText.text = "例如: https://api.openai.com/v1"
    }

    private val apiKeyField = JPasswordField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
    }

    private val contextTokensField = JBTextField("128000").apply {
        preferredSize = Dimension(JBUI.scale(150), preferredSize.height)
    }

    private val apiStyleComboBox = JComboBox(ModelApiStyle.entries.toTypedArray()).apply {
        preferredSize = Dimension(JBUI.scale(180), preferredSize.height)
    }

    private val reasoningEffortComboBox = JComboBox(REASONING_OPTIONS).apply {
        preferredSize = Dimension(JBUI.scale(180), preferredSize.height)
    }

    private val supportsImagesCheckBox = JBCheckBox("支持多模态(图片)", false)

    init {
        title = "添加自定义模型"
        apiStyleComboBox.addActionListener { updateCredentialFieldState() }
        updateCredentialFieldState()
        init()
    }

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
        addRow(form, c, 3, "API Key:", apiKeyField)
        addRow(form, c, 4, "上下文长度:", contextTokensField)
        addRow(form, c, 5, "接口类型:", apiStyleComboBox)
        addRow(form, c, 6, "Reasoning:", reasoningEffortComboBox)
        addRow(form, c, 7, "", supportsImagesCheckBox)

        return form
    }

    override fun doValidate(): ValidationInfo? {
        val modelName = modelNameField.text.trim()
        if (modelName.isBlank()) {
            return ValidationInfo("请输入模型名称", modelNameField)
        }
        if (isCustomModelExists(modelName)) {
            return ValidationInfo("模型 '$modelName' 已存在", modelNameField)
        }
        val apiStyle = apiStyleComboBox.selectedItem as? ModelApiStyle ?: ModelApiStyle.CHAT_COMPLETIONS
        if (apiStyle.requiresApiCredentials()) {
            if (endpointField.text.trim().isBlank()) {
                return ValidationInfo("请输入 API 地址", endpointField)
            }
            if (String(apiKeyField.password).trim().isBlank()) {
                return ValidationInfo("请输入 API Key", apiKeyField)
            }
        }
        val contextTokens = contextTokensField.text.toIntOrNull()
        if (contextTokens == null || contextTokens <= 0) {
            return ValidationInfo("上下文长度必须是大于 0 的整数", contextTokensField)
        }
        return null
    }

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

    private fun addRow(panel: JPanel, c: GridBagConstraints, row: Int, label: String, field: JComponent) {
        val labelConstraints = c.clone() as GridBagConstraints
        labelConstraints.gridx = 0
        labelConstraints.gridy = row
        labelConstraints.weightx = 0.0
        labelConstraints.anchor = GridBagConstraints.WEST
        panel.add(JBLabel(label), labelConstraints)

        val fieldConstraints = c.clone() as GridBagConstraints
        fieldConstraints.gridx = 1
        fieldConstraints.gridy = row
        fieldConstraints.weightx = 1.0
        panel.add(field, fieldConstraints)
    }

    private fun updateCredentialFieldState() {
        val apiStyle = apiStyleComboBox.selectedItem as? ModelApiStyle ?: ModelApiStyle.CHAT_COMPLETIONS
        val requiresCredentials = apiStyle.requiresApiCredentials()
        endpointField.isEnabled = requiresCredentials
        apiKeyField.isEnabled = requiresCredentials
        endpointField.emptyText.text = if (requiresCredentials) {
            "例如: https://api.openai.com/v1"
        } else {
            "Codex CLI 模式下不需要填写"
        }
        apiKeyField.toolTipText = if (requiresCredentials) {
            null
        } else {
            "Codex CLI 使用本机 codex login 登录态"
        }
    }

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
