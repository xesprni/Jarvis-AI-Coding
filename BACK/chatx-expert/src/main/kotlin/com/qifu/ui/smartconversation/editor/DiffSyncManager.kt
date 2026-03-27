package com.qifu.ui.smartconversation.editor

import com.intellij.diff.util.Side
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.qifu.ui.smartconversation.panels.ResponseEditorPanel.Companion.RESPONSE_EDITOR_DIFF_VIEWER_KEY
import com.qifu.ui.smartconversation.panels.ResponseEditorPanel.Companion.RESPONSE_EDITOR_DIFF_VIEWER_VALUE_PAIR_KEY
import kotlinx.coroutines.Dispatchers

object DiffSyncManager {

    val LOG = thisLogger()

    fun registerEditor(filePath: String, editor: EditorEx) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) } ?: return
        val listener = DiffSyncDocumentListener(filePath, editor)

        // 注册文档监听器
        val editorDisposable = Disposer.newDisposable("DiffSyncDocumentListener")
        EditorUtil.disposeWithEditor(editor, editorDisposable)
        document.addDocumentListener(listener, editorDisposable)
    }

}


class DiffSyncDocumentListener(val filePath: String, val editor: EditorEx) : DocumentListener {

    override fun documentChanged(event: DocumentEvent) {
        with(Dispatchers.Default) {
            val diffViewer = RESPONSE_EDITOR_DIFF_VIEWER_KEY.get(editor)
            if (diffViewer != null) {
                val leftSideDoc =
                    runReadAction { diffViewer.getDocument(Side.LEFT) }
                val rightSideDoc =
                    runReadAction { diffViewer.getDocument(Side.RIGHT) }
                // 检测左右文档是否相同
                if (leftSideDoc.text == rightSideDoc.text) {
                    return
                }

                // 当原文件被修改后，自动将diff视图右侧的“修改建议”同步应用到新内容上
                val entry = RESPONSE_EDITOR_DIFF_VIEWER_VALUE_PAIR_KEY.get(editor)
                if (entry != null) {
                    val (search, replace) = entry
                    val newText = event.document.text
                    if (!newText.contains(replace.trim())) {
                        val replacedText =
                            newText.replace(search.trim(), replace.trim())
                        runInEdt {
                            if (replacedText.length != newText.length) {
                                runUndoTransparentWriteAction {
                                    rightSideDoc.setText(
                                        StringUtil.convertLineSeparators(
                                            replacedText
                                        )
                                    )
                                    diffViewer.scheduleRediff()
                                }
                            }
                            diffViewer.rediff(true)
                        }
                    }
                }
            }
        }
    }
}
