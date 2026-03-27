package com.qifu.ui.smartconversation.sse

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.ui.ColorUtil
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import com.intellij.vcsUtil.VcsUtil.getVirtualFile
import com.qifu.agent.parser.ReplaceWaiting
import com.qifu.agent.parser.SearchReplace
import com.qifu.agent.parser.SearchWaiting
import com.qifu.agent.parser.Segment
import com.qifu.ui.smartconversation.editor.ComponentFactory
import com.qifu.ui.smartconversation.editor.DiffSyncManager
import com.qifu.ui.smartconversation.editor.ToolWindowEditorFileDetails
import com.qifu.ui.smartconversation.panels.ResponseEditorPanel
import com.qifu.ui.utils.EditorUtil
import com.qifu.utils.file.FileUtil
import javax.swing.JComponent

object EditorFactory {

    private val logger = thisLogger()

    fun createEditor(project: Project, segment: Segment, disposableParent: Disposable): EditorEx {
        val content = segment.content
        val languageMapping = FileUtil.findLanguageExtensionMapping(segment.language)
        val isDiffType = isDiffType(segment, content)
        return invokeAndWaitIfNeeded {
            val editor = if (isDiffType) {
                createDiffEditor(project, segment)
                    ?: EditorUtil.createEditor(project, languageMapping.value, content, disposableParent)
            } else {
                EditorUtil.createEditor(project, languageMapping.value, content, disposableParent)
            } as EditorEx
            segment.filePath?.let { filePath ->
                application.executeOnPooledThread {
                    Key.create<ToolWindowEditorFileDetails?>("jarvis.toolwindowEditorFileDetails").set(
                        editor,
                        ToolWindowEditorFileDetails(filePath, getVirtualFile(filePath))
                    )
                }
                DiffSyncManager.registerEditor(filePath, editor)
            }
            editor
        }
    }

    fun configureEditor(editor: EditorEx, headerComponent: JComponent? = null) {
        editor.permanentHeaderComponent = headerComponent
        editor.headerComponent = null

        val diffKind = editor.editorKind == EditorKind.DIFF
        editor.settings.apply {
            additionalColumnsCount = 0
            additionalLinesCount = 0
            isAdditionalPageAtBottom = false
            isVirtualSpace = false
            isUseSoftWraps = false
            isLineNumbersShown = diffKind
            isLineMarkerAreaShown = diffKind
        }
        editor.gutterComponentEx.apply {
            isVisible = diffKind
            parent.isVisible = diffKind
        }

        editor.contentComponent.border = JBUI.Borders.emptyLeft(4)
        editor.setBorder(JBUI.Borders.customLine(ColorUtil.fromHex("#48494b")))
        editor.installPopupHandler(
            ContextMenuPopupHandler.Simple(
                ComponentFactory.createEditorActionGroup(editor)
            )
        )
    }

    private fun createDiffEditor(project: Project, segment: Segment): EditorEx? {
        val filePath = segment.filePath
        if (filePath == null) {
            logger.warn("Cannot create diff editor for non-existent path")
            return null
        }

        val virtualFile = ApplicationManager.getApplication().executeOnPooledThread<VirtualFile?> {
            LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
        }.get() ?: return null
        val leftContent = DiffContentFactory.getInstance().create(project, virtualFile)

        val rightContentDoc = EditorFactory.getInstance()
            .createDocument(StringUtil.convertLineSeparators(virtualFile.readText()))
        rightContentDoc.setReadOnly(false)

        val rightContent =
            DiffContentFactory.getInstance().create(project, rightContentDoc, virtualFile)
        val diffRequest = SimpleDiffRequest(
            "Code Diff",
            listOf(leftContent, rightContent),
            listOf("Original", "Modified")
        )

        val diffViewer = UnifiedDiffViewer(MyDiffContext(project), diffRequest)
        ResponseEditorPanel.RESPONSE_EDITOR_DIFF_VIEWER_KEY.set(diffViewer.editor, diffViewer)
        return diffViewer.editor
    }

    private fun isDiffType(segment: Segment, content: String): Boolean {
        return segment is ReplaceWaiting
                || segment is SearchWaiting
                || segment is SearchReplace
                || content.startsWith("<<<")
    }


    class MyDiffContext(private val project: Project?) : DiffContext() {
        private val ownContext: UserDataHolder = UserDataHolderBase()

        override fun getProject() = project

        override fun isFocusedInWindow(): Boolean {
            return false
        }

        override fun isWindowFocused(): Boolean {
            return false
        }

        override fun requestFocusInWindow() {
        }

        override fun <T> getUserData(key: Key<T>): T? {
            return ownContext.getUserData(key)
        }

        override fun <T> putUserData(key: Key<T>, value: T?) {
            ownContext.putUserData(key, value)
        }
    }
}
