package com.qifu.ui.smartconversation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

sealed interface EditorNotifier {
    interface SelectionChange : EditorNotifier {
        fun selectionChanged(selectionModel: SelectionModel, virtualFile: VirtualFile)

        companion object {
            val TOPIC = Topic.create("jarvis.editorSelectionChanged", SelectionChange::class.java)
        }
    }

    interface Released : EditorNotifier {
        fun editorReleased(editor: Editor)

        companion object {
            val TOPIC = Topic.create("jarvis.editorReleased", Released::class.java)
        }
    }
}

class JarvisEditorFactoryListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        if (event.editor.editorKind != EditorKind.MAIN_EDITOR) {
            return
        }

        val project = event.editor.project ?: return
        val editorDisposable = Disposer.newDisposable("jarvisEditorSelectionDisposer")
        EditorUtil.disposeWithEditor(event.editor, editorDisposable)
        event.editor.selectionModel.addSelectionListener(object : SelectionListener {
            override fun selectionChanged(e: SelectionEvent) {
                if (!e.editor.contentComponent.hasFocus()) return
                val virtualFile = FileDocumentManager.getInstance().getFile(e.editor.document)?: return
                project.messageBus
                    .syncPublisher(EditorNotifier.SelectionChange.TOPIC)
                    .selectionChanged(e.editor.selectionModel, virtualFile)
            }
        }, editorDisposable)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        if (event.editor.editorKind != EditorKind.MAIN_EDITOR) {
            return
        }

        val project = event.editor.project ?: return
        project.messageBus
            .syncPublisher(EditorNotifier.Released.TOPIC)
            .editorReleased(event.editor)
    }
}