package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.miracle.services.ModelConfig
import com.miracle.services.isCustomModelExists

class EditCustomModelDialog(
    project: Project?,
    private val originalModel: ModelConfig
) : BaseCustomModelDialog(project, "API Key(留空不修改):") {

    init {
        modelNameField.text = originalModel.model
        aliasField.text = originalModel.alias
        endpointField.text = originalModel.endpoint
        contextTokensField.text = originalModel.contextTokens.toString()
        apiStyleComboBox.selectedItem = originalModel.resolvedApiStyle
        reasoningEffortComboBox.selectedItem = originalModel.resolvedReasoningEffort ?: "默认"
        supportsImagesCheckBox.isSelected = originalModel.supportsImages
        apiKeyField.toolTipText = "留空表示不修改已保存的 API Key"
        title = "编辑自定义模型"
        init()
    }

    override fun validateModelName(modelName: String): ValidationInfo? {
        if (modelName != originalModel.model && isCustomModelExists(modelName)) {
            return ValidationInfo("模型 '$modelName' 已存在", modelNameField)
        }
        return null
    }

    override fun validateApiKey(): ValidationInfo? {
        val newApiKey = String(apiKeyField.password).trim()
        if (newApiKey.isBlank() && originalModel.apiKey.isNullOrBlank()) {
            return ValidationInfo("请输入 API Key", apiKeyField)
        }
        return null
    }
}
