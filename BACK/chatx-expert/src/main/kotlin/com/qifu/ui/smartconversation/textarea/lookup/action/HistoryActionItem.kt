package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.HistoryTagDetails
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation
import javax.swing.Icon


/**
 * @author weiyichao
 * @date 2025-11-04
 **/
class HistoryActionItem(
    private val conversation: Conversation,
) : AbstractLookupActionItem() {

    companion object {
        fun getConversationTitle(conversation: Conversation): String {
            return conversation.messages.firstOrNull()?.let { firstMessage ->
                firstMessage.prompt?.take(60) ?: firstMessage.response?.take(60)
            } ?: "Conversation"
        }
    }

    override val displayName: String
        get() = getConversationTitle(conversation)

    override val icon: Icon
        get() = AllIcons.General.Balloon

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(
            HistoryTagDetails(
                conversationId = conversation.id,
                title = displayName,
            )
        )
    }
}