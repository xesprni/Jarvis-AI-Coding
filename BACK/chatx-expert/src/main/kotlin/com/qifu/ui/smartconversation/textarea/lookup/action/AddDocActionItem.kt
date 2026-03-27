package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.AddDocumentationDialog
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.settings.documentation.DocumentationSettings
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType
import com.qifu.ui.smartconversation.textarea.header.DocumentationTagDetails

class AddDocActionItem : AbstractLookupActionItem() {

    override val displayName: String =
        "Add new doc"
    override val icon = AllIcons.General.Add
    override val enabled = ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) == ServiceType.PROXYAI

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        val addDocumentationDialog = AddDocumentationDialog(project)
        if (addDocumentationDialog.showAndGet()) {
            service<DocumentationSettings>()
                .updateLastUsedDateTime(addDocumentationDialog.documentationDetails.url)
            userInputPanel.addTag(DocumentationTagDetails(addDocumentationDialog.documentationDetails))
        }
    }
}