package com.qifu.ui.smartconversation.textarea.header

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.qihoo.finance.lowcode.smartconversation.settings.SmartToolWindowContentManager

object TagUtil {
    fun <T : TagDetails> isTagTypePresent(
        project: Project,
        tagClass: Class<T>
    ): Boolean {
        return project.service<SmartToolWindowContentManager>()
            .tryFindActiveChatTabPanel()
            .map { it.selectedTags }
            .orElse(emptyList())
            .any { tagClass.isInstance(it) }
    }

    fun <T : TagDetails> getExistingTags(
        project: Project,
        tagClass: Class<T>
    ): List<T> {
        return project.service<SmartToolWindowContentManager>()
            .tryFindActiveChatTabPanel()
            .map { it.selectedTags }
            .orElse(emptyList())
            .filterIsInstance(tagClass)
    }
}