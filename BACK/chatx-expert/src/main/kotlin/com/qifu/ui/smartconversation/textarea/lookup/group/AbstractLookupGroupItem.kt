package com.qifu.ui.smartconversation.textarea.lookup.group

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.qifu.ui.smartconversation.textarea.lookup.AbstractLookupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupGroupItem

abstract class AbstractLookupGroupItem : AbstractLookupItem(), LookupGroupItem {

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.itemText = displayName
        presentation.icon = icon
        presentation.setTypeText("", AllIcons.Icons.Ide.NextStep)
        presentation.isTypeIconRightAligned = true
        presentation.isItemTextBold = false
    }

    override fun getLookupString(): String {
        return "group_${displayName.replace(" ", "_").lowercase()}"
    }
}