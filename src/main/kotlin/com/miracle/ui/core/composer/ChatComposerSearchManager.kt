package com.miracle.ui.core.composer

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.miracle.utils.RipgrepFileSearchService

/**
 * 聊天输入框的搜索管理器，负责搜索和构建文件/文件夹引用的补全项列表。
 *
 * @param project 当前项目实例
 */
class ChatComposerSearchManager(
    private val project: Project,
) {

    /**
     * 根据搜索文本搜索文件和文件夹引用补全项。
     * 当搜索文本为空时返回默认推荐项（最近打开的文件和常用文件夹），
     * 否则通过 ripgrep 执行模糊搜索。
     *
     * @param searchText 搜索关键词
     * @return 匹配的补全项列表
     */
    suspend fun searchReferences(searchText: String): List<ChatComposerLookupActionItem> {
        return if (searchText.isBlank()) {
            buildDefaultReferenceItems()
        } else {
            buildSearchReferenceItems(searchText)
        }
    }

    /**
     * 构建默认引用补全项列表，包括最近打开的文件和常用文件夹。
     *
     * @return 默认补全项列表
     */
    private suspend fun buildDefaultReferenceItems(): List<ChatComposerLookupActionItem> {
        val recentFiles = readAction {
            val fileEditorManager = FileEditorManager.getInstance(project)
            (fileEditorManager.selectedFiles.toList() + fileEditorManager.openFiles.toList())
                .distinctBy { it.path }
                .filter { !it.isDirectory }
                .take(6)
        }

        val folders = readAction {
            val root = project.guessProjectDir()
            val openFolders = recentFiles.mapNotNull { it.parent }
            listOfNotNull(root) + openFolders
        }.distinctBy { it.path }
            .take(4)

        return buildList {
            recentFiles.forEach { add(FileReferenceLookupItem(project, it)) }
            folders.forEach { add(FolderReferenceLookupItem(project, it)) }
        }.distinctBy { lookupIdentity(it) }
    }

    /**
     * 根据搜索文本通过 ripgrep 搜索匹配的文件和文件夹，构建补全项列表。
     *
     * @param searchText 搜索关键词
     * @return 搜索结果补全项列表
     */
    private suspend fun buildSearchReferenceItems(searchText: String): List<ChatComposerLookupActionItem> {
        val searchService = project.service<RipgrepFileSearchService>()
        val files = searchService.searchFiles(searchText, ignoreCase = true, maxResults = 12)
        val folders = searchService.listFolders(searchText, ignoreCase = true)
            .filter { it.path != project.basePath }
            .take(8)

        return buildList {
            files.forEach { add(FileReferenceLookupItem(project, it)) }
            folders.forEach { add(FolderReferenceLookupItem(project, it)) }
        }.distinctBy { lookupIdentity(it) }
    }

    companion object {
        /**
         * 生成补全项的唯一标识字符串，用于去重。
         *
         * @param item 补全项实例
         * @return 唯一标识字符串
         */
        fun lookupIdentity(item: ChatComposerLookupActionItem): String {
            return when (item) {
                is FileReferenceLookupItem -> "file:${item.filePath}"
                is FolderReferenceLookupItem -> "folder:${item.folderPath}"
                else -> "${item.javaClass.name}:${item.displayName}"
            }
        }
    }
}
