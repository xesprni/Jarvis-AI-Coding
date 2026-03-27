package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.AgentTagDetails
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons

class AgentActionItem(
    agentType: String,
    whenToUse: String,
) : AbstractLookupActionItem() {

    val description: String = whenToUse

    override val displayName: String = agentType
    override val icon = IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(AgentTagDetails(displayName))
    }

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        super.setPresentation(element, presentation)
        presentation.typeText = displayName
        presentation.isTypeGrayed = true
    }
}