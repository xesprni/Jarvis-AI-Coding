package com.qifu.ui.smartconversation.textarea.header

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.ui.smartconversation.settings.configuration.ConfigurationSettings
import com.qifu.ui.smartconversation.settings.configuration.ConfigurationStateListener
import java.util.concurrent.CopyOnWriteArraySet

class TagManager(parentDisposable: Disposable) {

    private val tags = mutableSetOf<TagDetails>()
    private val listeners = CopyOnWriteArraySet<TagManagerListener>()

    init {
        val connection = ApplicationManager.getApplication().messageBus
            .connect(parentDisposable)

        connection.subscribe(
            ConfigurationStateListener.TOPIC,
            ConfigurationStateListener { newState ->
                if (!newState.chatCompletionSettings.editorContextTagEnabled) {
                    clear()
                }
            })
    }

    fun addListener(listener: TagManagerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: TagManagerListener) {
        listeners.remove(listener)
    }

    fun getTags(): Set<TagDetails> = synchronized(this) { tags.toSet() }

    fun containsTag(file: VirtualFile): Boolean = tags.any {
        // TODO: refactor
        if (it is SelectionTagDetails) {
            it.virtualFile == file
        } else if (it is FileTagDetails) {
            it.virtualFile == file
        } else if (it is EditorSelectionTagDetails) {
            it.virtualFile == file
        } else if (it is EditorTagDetails) {
            it.virtualFile == file && it.selected
        } else {
            false
        }
    }

    fun addTag(tagDetails: TagDetails) {
        val wasAdded = synchronized(this) {
            if (!service<ConfigurationSettings>().state.chatCompletionSettings.editorContextTagEnabled
                && isEditorTag(tagDetails)
            ) return

            if (tagDetails is EditorSelectionTagDetails) {
                remove(tagDetails)
            }

            if (tags.count { !it.selected } == 3) {
                remove(tags.sortedBy { it.createdOn }.first { !it.selected })
            }

            tags.add(tagDetails)
        }
        if (wasAdded) {
            listeners.forEach { it.onTagAdded(tagDetails) }
        }
    }

    fun notifySelectionChanged(tagDetails: TagDetails) {
        val containsTag = synchronized(this) { tags.contains(tagDetails) }
        if (containsTag) {
            listeners.forEach { it.onTagSelectionChanged(tagDetails) }
        }
    }

    fun remove(tagDetails: TagDetails) {
        val wasRemoved = synchronized(this) { tags.remove(tagDetails) }
        if (wasRemoved) {
            listeners.forEach { it.onTagRemoved(tagDetails) }
        }
    }

    fun clear() {
        val removedTags = mutableListOf<TagDetails>()
        synchronized(this) {
            removedTags.addAll(tags)
            tags.clear()
        }
        removedTags.forEach { tag ->
            // 只移除标签,不清空编辑器的选中状态
            // if (tag is EditorSelectionTagDetails) {
            //     tag.selectionModel.removeSelection()
            // }
            listeners.forEach { it.onTagRemoved(tag) }
        }
    }

    private fun isEditorTag(tagDetails: TagDetails): Boolean =
        tagDetails is EditorSelectionTagDetails || tagDetails is EditorTagDetails
}
