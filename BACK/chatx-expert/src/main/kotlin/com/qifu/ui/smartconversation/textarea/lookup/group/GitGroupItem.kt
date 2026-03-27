package com.qifu.ui.smartconversation.textarea.lookup.group

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupUtil
import com.qifu.ui.smartconversation.textarea.lookup.files.GitCommitActionItem
import com.qifu.utils.GitUtil
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.Icon

class GitGroupItem(private val project: Project) : AbstractLookupGroupItem(), DynamicLookupGroupItem {

    override val displayName: String = "Git"
    override val icon: Icon = Icons.GIT

    override suspend fun updateLookupList(lookup: LookupImpl, searchText: String) {
        withContext(Dispatchers.Default) {
            GitUtil.getProjectRepository(project)?.let {
                GitUtil.visitRepositoryCommits(project, it) { commit ->
                    if (commit.id.asString().contains(searchText, true)
                        || commit.fullMessage.contains(searchText, true)
                    ) {
                        runInEdt {
                            LookupUtil.addLookupItem(lookup, GitCommitActionItem(commit))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> {
        return withContext(Dispatchers.Default) {
            GitUtil.getProjectRepository(project)?.let {
                val recentCommits = GitUtil.getAllRecentCommits(project, it, searchText)
                    .take(10)
                    .map { commit -> GitCommitActionItem(commit) }
                listOf(IncludeCurrentChangesActionItem()) + recentCommits
            } ?: emptyList()
        }
    }
}