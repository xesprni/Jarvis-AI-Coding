package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.miracle.services.isCustomModelExists

class AddCustomModelDialog(project: Project?) : BaseCustomModelDialog(project, "API Key:") {

    init {
        modelNameField.emptyText.text = "例如: gpt-4.1"
        aliasField.emptyText.text = "可选, 留空则使用模型名称"
        endpointField.emptyText.text = "例如: https://api.openai.com/v1"
        title = "添加自定义模型"
        init()
    }

    override fun validateModelName(modelName: String): ValidationInfo? {
        if (isCustomModelExists(modelName)) {
            return ValidationInfo("模型 '$modelName' 已存在", modelNameField)
        }
        return null
    }

    override fun validateApiKey(): ValidationInfo? {
        if (String(apiKeyField.password).trim().isBlank()) {
            return ValidationInfo("请输入 API Key", apiKeyField)
        }
        return null
    }
}
