package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.header.WebTagDetails

class WebActionItem(private val tagManager: TagManager) : AbstractLookupActionItem() {

    override val displayName: String =
        "web"
    override val icon = AllIcons.General.Web
    override val enabled: Boolean
        get() = enabled()

    fun enabled(): Boolean {
        if (ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) != ServiceType.PROXYAI) {
            return false
        }
        return tagManager.getTags().none { it is WebTagDetails }
    }

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(WebTagDetails())
    }
}