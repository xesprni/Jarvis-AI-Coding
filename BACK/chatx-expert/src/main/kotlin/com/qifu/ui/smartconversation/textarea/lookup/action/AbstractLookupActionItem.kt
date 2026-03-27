package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.qifu.ui.smartconversation.textarea.lookup.AbstractLookupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import java.util.UUID

abstract class AbstractLookupActionItem : AbstractLookupItem(), LookupActionItem {

    private val id: UUID = UUID.randomUUID()

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.icon = icon
        presentation.itemText = displayName
        presentation.isItemTextBold = false
    }

    override fun getLookupString(): String {
        return "action_${id}"
    }
}