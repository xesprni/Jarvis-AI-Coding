package com.qifu.ui.smartconversation.textarea.lookup.files

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.GitCommitTagDetails
import com.qifu.ui.smartconversation.textarea.lookup.action.AbstractLookupActionItem
import git4idea.GitCommit

class GitCommitActionItem(
    private val gitCommit: GitCommit,
) : AbstractLookupActionItem() {

    val description: String = gitCommit.id.asString().take(6)

    override val displayName: String = gitCommit.subject
    override val icon = AllIcons.Vcs.CommitNode

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        userInputPanel.addTag(GitCommitTagDetails(gitCommit))
    }
}