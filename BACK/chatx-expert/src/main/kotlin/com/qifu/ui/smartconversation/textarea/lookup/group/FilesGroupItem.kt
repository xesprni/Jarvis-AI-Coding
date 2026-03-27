package com.qifu.ui.smartconversation.textarea.lookup.group


import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.ui.smartconversation.textarea.header.FileTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.header.TagUtil
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupUtil
import com.qifu.ui.smartconversation.textarea.lookup.files.FileActionItem
import com.qifu.utils.RipgrepFileSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FilesGroupItem(
    private val project: Project,
    private val tagManager: TagManager
) : AbstractLookupGroupItem(), DynamicLookupGroupItem {

    override val displayName: String = "Files"
    override val icon = AllIcons.FileTypes.Any_type

    override suspend fun updateLookupList(lookup: LookupImpl, searchText: String) {
        withContext(Dispatchers.Default) {
            // 使用 ripgrep 快速列出文件
            val searchService = project.service<RipgrepFileSearchService>()
            val files = searchService.searchFiles(searchText, ignoreCase = true, maxResults = 100)

            files.forEach { file ->
                if (!containsTag(file)) {
                    val actionItem = FileActionItem(project, file)
                    runInEdt {
                        LookupUtil.addLookupItem(lookup, actionItem)
                    }
                }
            }
        }
    }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> {
        // 使用 ripgrep 快速搜索文件
        val searchService = project.service<RipgrepFileSearchService>()
        val matchingFiles = searchService.searchFiles(searchText, ignoreCase = true, maxResults = 100)

        return matchingFiles
            .filter { !containsTag(it) }
            .toFileSuggestions()
    }

    private fun containsTag(file: VirtualFile): Boolean {
        return tagManager.containsTag(file)
    }

    private fun Iterable<VirtualFile>.toFileSuggestions(): List<LookupActionItem> {
        val selectedFileTags = TagUtil.getExistingTags(project, FileTagDetails::class.java)
        return filter { file -> selectedFileTags.none { it.virtualFile == file } }
            .map { FileActionItem(project, it) }
    }
}