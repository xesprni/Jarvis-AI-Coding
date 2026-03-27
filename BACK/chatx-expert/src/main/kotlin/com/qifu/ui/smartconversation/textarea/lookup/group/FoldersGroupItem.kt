package com.qifu.ui.smartconversation.textarea.lookup.group

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupUtil
import com.qifu.ui.smartconversation.textarea.lookup.files.FolderActionItem
import com.qifu.ui.smartconversation.textarea.lookup.group.AbstractLookupGroupItem
import com.qifu.utils.RipgrepFileSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoldersGroupItem(
    private val project: Project,
    private val tagManager: TagManager
) : AbstractLookupGroupItem(), DynamicLookupGroupItem {

    override val displayName: String = "Folders"
    override val icon = AllIcons.Nodes.Folder

    override suspend fun updateLookupList(lookup: LookupImpl, searchText: String) {
        withContext(Dispatchers.Default) {
            // 使用 ripgrep 快速列出文件夹
            val searchService = project.service<RipgrepFileSearchService>()
            val folders = searchService.listFolders(searchText, ignoreCase = true)
            
            folders.forEach { folder ->
                if (!tagManager.containsTag(folder)) {
                    runInEdt {
                        LookupUtil.addLookupItem(lookup, FolderActionItem(project, folder))
                    }
                }
            }
        }
    }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> {
        // 使用 ripgrep 快速列出文件夹
        val searchService = project.service<RipgrepFileSearchService>()
        val folders = searchService.listFolders(searchText, ignoreCase = true)
        return folders.toFolderSuggestions()
    }

    private fun Iterable<VirtualFile>.toFolderSuggestions() =
        take(10).map { FolderActionItem(project, it) }
}