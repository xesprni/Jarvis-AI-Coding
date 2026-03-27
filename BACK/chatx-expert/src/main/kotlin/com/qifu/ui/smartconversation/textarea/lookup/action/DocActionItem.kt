package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.DocumentationDetails
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.settings.documentation.DocumentationSettings
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType
import com.qifu.ui.smartconversation.textarea.header.DocumentationTagDetails

class DocActionItem(
    private val documentationDetails: DocumentationDetails
) : AbstractLookupActionItem() {

    override val displayName = documentationDetails.name
    override val icon = AllIcons.Toolwindows.Documentation
    override val enabled = ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) == ServiceType.PROXYAI

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        super.setPresentation(element, presentation)

        presentation.typeText = documentationDetails.url
        presentation.isTypeGrayed = true
    }

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        service<DocumentationSettings>().updateLastUsedDateTime(documentationDetails.url)
        userInputPanel.addTag(DocumentationTagDetails(documentationDetails))
    }
}