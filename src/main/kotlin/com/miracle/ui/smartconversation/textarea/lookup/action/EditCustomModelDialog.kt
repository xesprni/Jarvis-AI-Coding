package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelApiStyle
import com.miracle.services.ModelConfig
import com.miracle.services.isCustomModelExists
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPasswordField

class EditCustomModelDialog(
    project: Project?,
    private val originalModel: ModelConfig
) : DialogWrapper(project) {

    private val modelNameField = JBTextField(originalModel.model).apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
    }

    private val aliasField = JBTextField(originalModel.alias).apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
    }

    private val endpointField = JBTextField(originalModel.endpoint).apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
    }

    private val apiKeyField = JPasswordField().apply {
        preferredSize = Dimension(JBUI.scale(400), preferredSize.height)
        toolTipText = "留空表示不修改已保存的 API Key"
    }

    private val contextTokensField = JBTextField(originalModel.contextTokens.toString()).apply {
        preferredSize = Dimension(JBUI.scale(150), preferredSize.height)
    }

    private val apiStyleComboBox = JComboBox(ModelApiStyle.entries.toTypedArray()).apply {
        preferredSize = Dimension(JBUI.scale(180), preferredSize.height)
        selectedItem = originalModel.resolvedApiStyle
    }

    private val reasoningEffortComboBox = JComboBox(AddCustomModelDialog.REASONING_OPTIONS).apply {
        preferredSize = Dimension(JBUI.scale(180), preferredSize.height)
        selectedItem = originalModel.resolvedReasoningEffort ?: "默认"
    }

    private val supportsImagesCheckBox = JBCheckBox("支持多模态(图片)", originalModel.supportsImages)

    init {
        title = "编辑自定义模型"
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
        addRow(form, c, 3, "API Key(留空不修改):", apiKeyField)
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
        if (modelName != originalModel.model && isCustomModelExists(modelName)) {
            return ValidationInfo("模型 '$modelName' 已存在", modelNameField)
        }
        val apiStyle = apiStyleComboBox.selectedItem as? ModelApiStyle ?: ModelApiStyle.CHAT_COMPLETIONS
        if (endpointField.text.trim().isBlank()) {
            return ValidationInfo("请输入 API 地址", endpointField)
        }
        val newApiKey = String(apiKeyField.password).trim()
        if (newApiKey.isBlank() && originalModel.apiKey.isNullOrBlank()) {
            return ValidationInfo("请输入 API Key", apiKeyField)
        }
        val contextTokens = contextTokensField.text.toIntOrNull()
        if (contextTokens == null || contextTokens <= 0) {
            return ValidationInfo("上下文长度必须是大于 0 的整数", contextTokensField)
        }
        return null
    }

    fun getModelInfo(): AddCustomModelDialog.ModelInfo {
        return AddCustomModelDialog.ModelInfo(
            model = modelNameField.text.trim(),
            endpoint = endpointField.text.trim(),
            apiKey = String(apiKeyField.password).trim(),
            contextTokens = contextTokensField.text.toInt(),
            alias = aliasField.text.trim(),
            apiStyle = apiStyleComboBox.selectedItem as? ModelApiStyle ?: ModelApiStyle.CHAT_COMPLETIONS,
            reasoningEffort = AddCustomModelDialog.parseReasoningEffort(reasoningEffortComboBox.selectedItem as? String),
            supportsImages = supportsImagesCheckBox.isSelected
        )
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

    private fun updateCredentialFieldState() {
        endpointField.isEnabled = true
        apiKeyField.isEnabled = true
    }
}
