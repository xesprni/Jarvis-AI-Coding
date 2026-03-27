package com.miracle.ui.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI.Fonts
import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.DROPDOWN_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_BACKGROUND
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_BORDER_COLOR
import com.miracle.ui.core.ChatTheme.DROPDOWN_ROW_HOVER_BACKGROUND
import com.miracle.ui.core.ChatTheme.MUTED_FOREGROUND
import com.miracle.utils.RipgrepFileSearchService
import com.miracle.utils.toRelativePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants

internal class AssociatedFilePickerPopup(
    private val project: Project,
) {

    private data class FilePickerItem(
        val file: VirtualFile,
        val alreadyAssociated: Boolean,
    ) {
        val displayName: String
            get() = file.name
    }

    fun show(
        anchor: Component,
        existingPaths: Set<String>,
        onFileSelected: (VirtualFile) -> Unit,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val searchField = JBTextField().apply {
            emptyText.text = "搜索项目文件"
        }
        val listModel = DefaultListModel<FilePickerItem>()
        val list = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            visibleRowCount = 8
            fixedCellHeight = JBUI.scale(34)
            border = JBUI.Borders.empty(2)
            background = DROPDOWN_BACKGROUND
            cellRenderer = FilePickerCellRenderer(project)
        }
        val emptyLabel = JBLabel("输入文件名或路径搜索").apply {
            font = JBFont.small()
            foreground = MUTED_FOREGROUND
            border = JBUI.Borders.empty(0, 2, 0, 2)
        }
        val listPane = JBScrollPane(list).apply {
            border = JBUI.Borders.customLine(DROPDOWN_BORDER_COLOR, 1)
            preferredSize = Dimension(JBUI.scale(420), JBUI.scale(256))
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            viewport.background = DROPDOWN_BACKGROUND
        }
        val contentPanel = JPanel(BorderLayout(JBUI.scale(0), JBUI.scale(6))).apply {
            isOpaque = true
            background = DROPDOWN_BACKGROUND
            border = JBUI.Borders.compound(
                createRoundedBorder(DROPDOWN_BORDER_COLOR),
                JBUI.Borders.empty(8),
            )
            add(searchField, BorderLayout.NORTH)
            add(listPane, BorderLayout.CENTER)
            add(emptyLabel, BorderLayout.SOUTH)
        }

        var popup: JBPopup? = null
        var searchJob: Job? = null

        fun chooseSelectedItem() {
            val selected = list.selectedValue ?: return
            popup?.cancel()
            onFileSelected(selected.file)
        }

        fun updateEmptyText() {
            emptyLabel.text = when {
                searchField.text.isBlank() && listModel.isEmpty -> "没有可选择的最近文件"
                listModel.isEmpty -> "没有匹配的文件"
                else -> "Enter 选择，Esc 关闭"
            }
        }

        fun loadItems(query: String) {
            searchJob?.cancel()
            searchJob = scope.launch {
                if (query.isNotBlank()) {
                    delay(160)
                }
                val files = if (query.isBlank()) {
                    ApplicationManager.getApplication().runReadAction<List<VirtualFile>> {
                        val fileEditorManager = FileEditorManager.getInstance(project)
                        (fileEditorManager.selectedFiles.toList() + fileEditorManager.openFiles.toList())
                            .filter { !it.isDirectory }
                            .distinctBy { it.path }
                            .take(20)
                    }
                } else {
                    project.service<RipgrepFileSearchService>()
                        .searchFiles(query, ignoreCase = true, maxResults = 30)
                        .distinctBy { it.path }
                }
                val items = files.map { file ->
                    FilePickerItem(file = file, alreadyAssociated = existingPaths.contains(file.path))
                }
                ApplicationManager.getApplication().invokeLater {
                    listModel.removeAllElements()
                    items.forEach(listModel::addElement)
                    if (!listModel.isEmpty) {
                        list.selectedIndex = 0
                    }
                    updateEmptyText()
                }
            }
        }

        searchField.document.addDocumentListener(SimpleDocumentListener {
            loadItems(searchField.text.trim())
        })
        searchField.addActionListener { chooseSelectedItem() }
        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    chooseSelectedItem()
                }
            }
        })
        list.inputMap.put(KeyStroke.getKeyStroke("ENTER"), "choose-associated-file")
        list.actionMap.put("choose-associated-file", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                chooseSelectedItem()
            }
        })

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, searchField)
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setMovable(false)
            .setResizable(false)
            .setCancelCallback {
                scope.cancel()
                true
            }
            .createPopup()

        loadItems("")
        popup.showUnderneathOf(anchor)
        ApplicationManager.getApplication().invokeLater {
            searchField.requestFocusInWindow()
        }
    }

    private class FilePickerCellRenderer(
        private val project: Project,
    ) : JPanel(), ListCellRenderer<FilePickerItem> {
        private val titleLabel = JBLabel().apply { font = JBFont.label().asBold() }
        private val statusLabel = JBLabel().apply {
            font = Fonts.miniFont()
            foreground = MUTED_FOREGROUND
        }
        private val pathLabel = JBLabel().apply {
            font = Fonts.miniFont()
            foreground = MUTED_FOREGROUND
        }
        private val headerPanel = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(titleLabel, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.EAST)
        }

        init {
            layout = BorderLayout(0, JBUI.scale(1))
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(DROPDOWN_ROW_BORDER_COLOR, 0, 0, 1, 0),
                JBUI.Borders.empty(2, 8, 1, 8),
            )
            isOpaque = true
            add(headerPanel, BorderLayout.NORTH)
            add(pathLabel, BorderLayout.CENTER)
        }

        override fun getListCellRendererComponent(
            list: JList<out FilePickerItem>,
            value: FilePickerItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val item = value ?: return this

            val basePath = project.basePath
            val relativePath = if (basePath.isNullOrBlank()) {
                item.file.path
            } else {
                toRelativePath(item.file.path, basePath)
            }
            val background = if (isSelected) DROPDOWN_ROW_HOVER_BACKGROUND else DROPDOWN_ROW_BACKGROUND

            this.background = background
            titleLabel.icon = item.file.fileType.icon
            titleLabel.text = item.displayName
            titleLabel.foreground = list.foreground
            statusLabel.text = if (item.alreadyAssociated) "已关联" else ""
            pathLabel.text = relativePath
            pathLabel.foreground = MUTED_FOREGROUND
            return this
        }
    }

    private fun interface SimpleDocumentListener : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onChanged()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onChanged()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onChanged()

        fun onChanged()
    }
}
