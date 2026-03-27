package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.qifu.utils.GitUtil
import com.qihoo.finance.lowcode.common.util.Icons
import com.qihoo.finance.lowcode.smartconversation.settings.SmartToolWindowContentManager
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExplainGitCommitAction : AnAction(
    "Explain Commit With Jarvis",
    "Generate commit explanation with Jarvis",
    Icons.LOGO_ROUND13
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        scope.launch {
            val gitCommits =
                getCommitsForRevisions(project, e.getData(VcsDataKeys.VCS_REVISION_NUMBERS))

            project.service<SmartToolWindowContentManager>().apply {
                runInEdt {
                    displayChatTab()
                    tryFindActiveChatTabPanel()
                        .ifPresent {
                            it.addCommitReferences(gitCommits)
                        }
                }
            }
        }
    }

    private fun getCommitsForRevisions(
        project: Project,
        revisionNumbers: Array<VcsRevisionNumber>?
    ): List<GitCommit> {
        if (revisionNumbers == null) {
            throw IllegalArgumentException("No commit revisions found")
        }

        val gitCommits = GitUtil.getProjectRepository(project)?.let { repository ->
            GitUtil.getCommitsForHashes(
                project,
                repository,
                revisionNumbers.map { it.asString() })
        } ?: throw IllegalStateException("Unable to find git repository")

        if (gitCommits.isEmpty()) {
            throw IllegalStateException(
                "Unable to find commits for given revisions: ${
                    revisionNumbers.joinToString(",") { it.asString() }
                }"
            )
        }

        return gitCommits
    }
}