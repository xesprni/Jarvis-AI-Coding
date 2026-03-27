package com.miracle.ui.core

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

class AddToJarvisChatAction : AnAction("发送选中内容到 Jarvis"), DumbAware {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = project != null && editor != null && editor.selectionModel.hasSelection()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val text = editor.selectionModel.selectedText?.trim()
        if (text.isNullOrBlank()) {
            Messages.showInfoMessage(project, "没有可发送的选中内容。", "Jarvis")
            return
        }

        val virtualFile = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)
        if (virtualFile == null) {
            project.getService(JarvisToolWindowService::class.java).appendSelection(text)
            return
        }

        val startLine = document.getLineNumber(editor.selectionModel.selectionStart)
        val endLine = document.getLineNumber(editor.selectionModel.selectionEnd)
        val startOffset = document.getLineStartOffset(startLine)
        val endOffset = document.getLineEndOffset(endLine)
        val fullLineText = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))

        project.getService(JarvisToolWindowService::class.java).appendAssociatedCodeSelection(
            filePath = virtualFile.path,
            startLine = startLine + 1,
            endLine = endLine + 1,
            fullLineText = fullLineText,
        )
    }
}
