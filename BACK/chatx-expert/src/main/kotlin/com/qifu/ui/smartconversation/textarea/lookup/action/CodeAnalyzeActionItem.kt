package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.CodeAnalyzeTagDetails
import com.qifu.ui.smartconversation.textarea.header.EditorTagDetails
import com.qifu.ui.smartconversation.textarea.header.FileTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagManager

class CodeAnalyzeActionItem(
    private val tagManager: TagManager
) : AbstractLookupActionItem() {

    override val displayName: String = "Code Analyze"
    override val icon = AllIcons.Actions.DependencyAnalyzer
    override val enabled: Boolean
        get() = tagManager.getTags().none { it is CodeAnalyzeTagDetails } &&
                tagManager.getTags().any { it is FileTagDetails || it is EditorTagDetails }


    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(CodeAnalyzeTagDetails())
    }
}