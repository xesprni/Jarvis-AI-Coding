package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType


class ViewAllDocsActionItem : AbstractLookupActionItem() {

    override val displayName: String =
        "${"view all docs"} →"
    override val icon = null
    override val enabled = ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) == ServiceType.PROXYAI

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
//        service<ShowSettingsUtil>().showSettingsDialog(
//            project,
//            DocumentationsConfigurable::class.java
//        )
    }
}