package com.miracle.ui.core.composer

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.miracle.utils.RipgrepFileSearchService

class ChatComposerSearchManager(
    private val project: Project,
) {

    suspend fun searchReferences(searchText: String): List<ChatComposerLookupActionItem> {
        return if (searchText.isBlank()) {
            buildDefaultReferenceItems()
        } else {
            buildSearchReferenceItems(searchText)
        }
    }

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
        fun lookupIdentity(item: ChatComposerLookupActionItem): String {
            return when (item) {
                is FileReferenceLookupItem -> "file:${item.filePath}"
                is FolderReferenceLookupItem -> "folder:${item.folderPath}"
                else -> "${item.javaClass.name}:${item.displayName}"
            }
        }
    }
}
