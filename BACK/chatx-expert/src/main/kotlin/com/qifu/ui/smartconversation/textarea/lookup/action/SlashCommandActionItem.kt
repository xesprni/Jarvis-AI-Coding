package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.ui.JBColor
import com.qifu.ui.smartconversation.SlashCommand
import com.qifu.ui.smartconversation.SlashCommandCategory
import com.qifu.ui.smartconversation.textarea.lookup.AbstractLookupItem
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons
import java.awt.Color

class SlashCommandActionItem(
    val command: SlashCommand,
) : AbstractLookupItem() {

    override val displayName: String = command.command
    override val icon = command.icon ?: IconUtil.getThemeAwareIcon(Icons.AI_COMMAND_LIGHT, Icons.AI_COMMAND)

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.icon = icon
        presentation.itemText = command.command
        if (command.argumentTemplates.isNotEmpty()) {
            presentation.appendTailText("  ${command.argumentTemplates.joinToString(" ")}", false)
        }
        if (command.description.isNotBlank()) {
            presentation.appendTailText("  ${command.description}", true)
        }
        presentation.typeText = command.category.displayName
        presentation.isTypeGrayed = true
        presentation.isItemTextBold = true
        presentation.itemTextForeground = when (command.category) {
            SlashCommandCategory.BUILT_IN -> JBColor(
                Color(0x2E7D32),
                Color(0x81C784)
            )

            SlashCommandCategory.SKILL -> JBColor(
                Color(0x1565C0),
                Color(0x90CAF9)
            )
        }
    }

    override fun getLookupString(): String {
        return command.command
    }
}
