package com.qifu.ui.smartconversation.action

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.qifu.ui.utils.CompletionRequestUtil
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory
import com.qihoo.finance.lowcode.common.util.NotifyUtils
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel
import org.jetbrains.annotations.NotNull

/**
 * 添加选中代码到 Jarvis Chat 的 Action
 * 用于在代码浮动工具栏中显示"Add to Jarvis Chat"按钮
 */
class AddToJarvisChatAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        sendToJarvisChat(project, editor)
    }

    override fun update(@NotNull e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        // 只有当有项目、编辑器且有选中文本时才显示此 Action
        e.presentation.isEnabledAndVisible = project != null 
            && editor != null 
            && editor.selectionModel.hasSelection()
    }

    private fun sendToJarvisChat(project: Project, editor: Editor) {
        // 检查是否有选中的文本
        if (!editor.selectionModel.hasSelection()) return
        
        val selectedText = editor.selectionModel.selectedText ?: return
        if (selectedText.isBlank()) return
        
        val virtualFile = editor.virtualFile
        val fileName = virtualFile?.name ?: "Unknown"
        
        // 获取选中的行号范围
        val document = editor.document
        val selectionModel = editor.selectionModel
        val startLine = document.getLineNumber(selectionModel.selectionStart) + 1  // 行号从1开始
        val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
        val lineStartPos = document.getLineStartOffset(startLine - 1)
        val lineEndPos = document.getLineEndOffset(endLine - 1)
        val selectedCode = document.getText(TextRange(lineStartPos, lineEndPos))
        val codeBlock = CompletionRequestUtil.formatCodeBlock(
            virtualFile.path,
            project.basePath?:"",
            selectedCode,
            startLine,
            endLine
        )

        ChatXToolWindowFactory.showFirstTab()
        val content = ChatXToolWindowFactory.getToolWindow().contentManager.selectedContent
        if (content == null) {
            NotifyUtils.notify("Send to jarvis chat failed.", NotificationType.WARNING)
            return
        }
        if (content.component is SmartToolWindowPanel) {
            val smartToolWindowPanel = content.component as SmartToolWindowPanel
            smartToolWindowPanel.chatTabbedPane.tryFindActiveTabPanel().ifPresent { chatTabPanel ->
                // 插入代码选择占位符，显示为：文件名 (开始行号-结束行号)
                chatTabPanel.insertCodeSelectionPlaceholder(fileName, startLine, endLine, codeBlock)
                // 移除 EditorSelectionTagDetails,但保持编辑器中的选中状态
                chatTabPanel.removeEditorSelectionTag()
            }
        }
    }
}
