package com.miracle.ui.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

@Service(Service.Level.PROJECT)
class JarvisToolWindowService(private val project: Project) {
    var panel: JarvisToolWindowPanel? = null
    private val pendingInsertions = mutableListOf<ChatComposerInsertion>()
    private val pendingAssociatedFiles = mutableListOf<String>()
    private val pendingAssociatedCodeSelections = mutableListOf<AssociatedContextItem.AssociatedCodeSelection>()

    fun bind(panel: JarvisToolWindowPanel) {
        this.panel = panel
        if (pendingAssociatedFiles.isNotEmpty()) {
            panel.appendAssociatedFiles(pendingAssociatedFiles.toList())
            pendingAssociatedFiles.clear()
        }
        if (pendingAssociatedCodeSelections.isNotEmpty()) {
            panel.appendAssociatedCodeSelections(pendingAssociatedCodeSelections.toList())
            pendingAssociatedCodeSelections.clear()
        }
        if (pendingInsertions.isNotEmpty()) {
            panel.appendInsertions(pendingInsertions.toList())
            pendingInsertions.clear()
        }
    }

    fun unbind(panel: JarvisToolWindowPanel) {
        if (this.panel === panel) {
            this.panel = null
        }
    }

    fun showToolWindow(): ToolWindow? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(JarvisToolWindowFactory.TOOL_WINDOW_ID)
        toolWindow?.show()
        return toolWindow
    }

    internal fun showSettings(section: JarvisSettingsSection) {
        showToolWindow()?.show {
            panel?.showSettingsOverlay(section)
        }
    }

    fun appendSelection(text: String) {
        appendInsertion(ChatComposerInsertion.PlainText(text))
    }

    fun appendCodeReference(
        filePath: String,
        startLine: Int,
        endLine: Int,
        code: String,
    ) {
        appendInsertion(
            ChatComposerInsertion.CodeReference(
                filePath = filePath,
                startLine = startLine,
                endLine = endLine,
                code = code,
            )
        )
    }

    fun appendPathReference(path: String, directory: Boolean = false) {
        appendInsertion(ChatComposerInsertion.PathReference(path = path, directory = directory))
    }

    fun appendAssociatedFile(path: String) {
        pendingAssociatedFiles += path
        showToolWindow()?.show {
            val boundPanel = panel
            if (boundPanel != null && pendingAssociatedFiles.isNotEmpty()) {
                boundPanel.appendAssociatedFiles(pendingAssociatedFiles.toList())
            }
            pendingAssociatedFiles.clear()
        }
    }

    fun appendAssociatedCodeSelection(
        filePath: String,
        startLine: Int,
        endLine: Int,
        fullLineText: String,
    ) {
        pendingAssociatedCodeSelections += AssociatedContextItem.AssociatedCodeSelection(
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            fullLineText = fullLineText,
        )
        showToolWindow()?.show {
            val boundPanel = panel
            if (boundPanel != null && pendingAssociatedCodeSelections.isNotEmpty()) {
                boundPanel.appendAssociatedCodeSelections(pendingAssociatedCodeSelections.toList())
            }
            pendingAssociatedCodeSelections.clear()
        }
    }

    fun appendInsertion(insertion: ChatComposerInsertion) {
        pendingInsertions += insertion
        showToolWindow()?.show {
            val boundPanel = panel
            if (boundPanel != null) {
                if (pendingInsertions.isNotEmpty()) {
                    boundPanel.appendInsertions(pendingInsertions.toList())
                }
            }
            pendingInsertions.clear()
        }
    }
}
