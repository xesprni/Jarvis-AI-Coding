package com.qifu.ui.smartconversation.textarea

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.codeStyle.MinusculeMatcher
import com.intellij.psi.codeStyle.NameUtil
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupItem
import com.qifu.ui.smartconversation.textarea.lookup.action.ImageActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.WebActionItem
import com.qifu.ui.smartconversation.textarea.lookup.files.FileActionItem
import com.qifu.ui.smartconversation.textarea.lookup.group.AgentGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.group.FilesGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.group.FoldersGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.group.GitGroupItem
import kotlinx.coroutines.CancellationException

data class SearchState(
    val isInSearchContext: Boolean = false,
    val isInGroupLookupContext: Boolean = false,
    val lastSearchText: String? = null
)

class SearchManager(
    private val project: Project,
    private val tagManager: TagManager,
    private val taskId: String,
) {
    companion object {
        private val logger = thisLogger()
        private var supportImages = false
    }

    fun getDefaultGroups() = listOf(
        FilesGroupItem(project, tagManager),
        FoldersGroupItem(project, tagManager),
        GitGroupItem(project),
        AgentGroupItem(project, tagManager),
        ImageActionItem(taskId)
    ).filter {
        !(it is ImageActionItem && !supportImages)
    }

    fun setSupportImages(value: Boolean) {
        supportImages = value
    }

    /**
     * 执行全局搜索
     * 
     * @param searchText 搜索文本
     * @return 搜索结果列表,可能包含文件、文件夹或分组菜单
     */
    suspend fun performGlobalSearch(searchText: String): List<LookupItem> {
        // 当 searchText 为空时,返回当前激活的文件、最近打开的文件和常规菜单
        if (searchText.isEmpty()) {
            val recentFiles: List<LookupItem> = getRecentOpenFiles()
            val menuGroups: List<LookupItem> = getDefaultGroups().filterIsInstance<LookupGroupItem>()
            return recentFiles + menuGroups
        }
        
        // 有搜索内容时,搜索文件和文件夹
        val allGroups = getDefaultGroups().filterNot { it is ImageActionItem }
        val allResults = mutableListOf<LookupActionItem>()

        allGroups.forEach { group ->
            try {
                if (group is LookupGroupItem) {
                    // 只搜索文件和文件夹,传递 searchText 参数
                    if (group is FilesGroupItem || group is FoldersGroupItem) {
                        val lookupActionItems = group.getLookupItems(searchText).filterIsInstance<LookupActionItem>()
                        allResults.addAll(lookupActionItems)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Error getting results from ${group::class.simpleName}", e)
            }
        }
        return filterAndSortResults(allResults, searchText)
    }

    /**
     * 获取最近打开的文件(当前激活的文件 + 最近打开的2个文件)
     */
    private suspend fun getRecentOpenFiles(): List<LookupActionItem> {
        return readAction {
            val fileEditorManager = project.service<FileEditorManager>()
            val projectFileIndex = project.service<ProjectFileIndex>()
            
            // 获取当前选中的文件
            val selectedFiles = fileEditorManager.selectedFiles.toList()
            
            // 获取所有打开的文件
            val openFiles = fileEditorManager.openFiles.toList()
            
            // 优先返回选中的文件,然后是其他打开的文件,最多3个
            val filesToShow = (selectedFiles + openFiles)
                .distinctBy { it.path }
                .filter { 
                    projectFileIndex.isInContent(it) && 
                    !tagManager.containsTag(it) 
                }
                .take(3)
            
            filesToShow.map { FileActionItem(project, it) }
        }
    }

    private fun filterAndSortResults(
        results: List<LookupActionItem>, searchText: String
    ): List<LookupActionItem> {
        val matcher: MinusculeMatcher = NameUtil.buildMatcher("*$searchText").build()

        return results.mapNotNull { result ->
            when (result) {
                is WebActionItem -> {
                    if (searchText.contains("web", ignoreCase = true)) {
                        result to 100
                    } else null
                }

                else -> {
                    val matchingDegree = matcher.matchingDegree(result.displayName)
                    if (matchingDegree != Int.MIN_VALUE) {
                        result to matchingDegree
                    } else null
                }
            }
        }.sortedByDescending { it.second }.map { it.first }.take(PromptTextFieldConstants.MAX_SEARCH_RESULTS)
    }

    fun getSearchTextAfterAt(text: String, caretOffset: Int): String? {
        val atPos = text.lastIndexOf(PromptTextFieldConstants.AT_SYMBOL)
        if (atPos == -1 || atPos >= caretOffset) return null

        val searchText = text.substring(atPos + 1, caretOffset)
        return if (searchText.contains(PromptTextFieldConstants.SPACE) || searchText.contains(PromptTextFieldConstants.NEWLINE)) {
            null
        } else {
            searchText
        }
    }

    fun matchesAnyDefaultGroup(searchText: String): Boolean {
        return PromptTextFieldConstants.DEFAULT_GROUP_NAMES.any { groupName ->
            groupName.startsWith(searchText, ignoreCase = true)
        }
    }
}