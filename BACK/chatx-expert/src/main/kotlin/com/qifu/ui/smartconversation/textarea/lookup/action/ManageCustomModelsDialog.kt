package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.qifu.services.ModelConfig
import com.qifu.services.deleteCustomModel
import com.qifu.services.getCustomModels
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer

/**
 * 管理自定义模型对话框
 */
class ManageCustomModelsDialog(
    private val project: Project?,
    private val onModelChanged: () -> Unit
) : DialogWrapper(project) {

    private val tableModel = CustomModelsTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowHeight = JBUI.scale(40)
        
        // 设置列宽
        columnModel.getColumn(0).preferredWidth = JBUI.scale(150) // 模型名称
        columnModel.getColumn(1).preferredWidth = JBUI.scale(120) // 显示名称
        columnModel.getColumn(2).preferredWidth = JBUI.scale(250) // API地址
        columnModel.getColumn(3).preferredWidth = JBUI.scale(120) // 上下文
        columnModel.getColumn(4).preferredWidth = JBUI.scale(80)  // 多模态
        columnModel.getColumn(5).preferredWidth = JBUI.scale(160) // 操作
        columnModel.getColumn(5).minWidth = JBUI.scale(160)
        
        // 设置操作列的渲染器和编辑器
        columnModel.getColumn(5).cellRenderer = ButtonPanelRenderer()
        columnModel.getColumn(5).cellEditor = ButtonPanelEditor(this@ManageCustomModelsDialog)
    }
    
    private val addButton = JButton("添加模型", AllIcons.General.Add).apply {
        addActionListener {
            showAddModelDialog()
        }
    }

    init {
        title = "管理自定义模型"
        init()
        loadModels()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(JBUI.scale(10), JBUI.scale(10))).apply {
            preferredSize = Dimension(JBUI.scale(800), JBUI.scale(500))
        }
        
        // 表格区域
        val scrollPane = JBScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 底部按钮
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(addButton)
            add(Box.createHorizontalGlue())
        }
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        // 如果没有数据，显示提示
        if (tableModel.rowCount == 0) {
            val emptyLabel = JLabel("暂无自定义模型，点击下方按钮添加", SwingConstants.CENTER).apply {
                foreground = java.awt.Color.GRAY
            }
            panel.add(emptyLabel, BorderLayout.NORTH)
        }
        
        return panel
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }

    override fun getOKAction(): Action {
        return super.getOKAction().apply {
            putValue(Action.NAME, "关闭")
        }
    }

    private fun loadModels() {
        tableModel.loadModels()
    }

    private fun showAddModelDialog() {
        val dialog = AddCustomModelDialog(project)
        if (dialog.showAndGet()) {
            val modelInfo = dialog.getModelInfo()
            try {
                com.qifu.services.addCustomModel(
                    model = modelInfo.model,
                    endpoint = modelInfo.endpoint,
                    apiKey = modelInfo.apiKey,
                    contextTokens = modelInfo.contextTokens,
                    alias = modelInfo.alias.takeIf { it.isNotBlank() },
                    supportsImages = modelInfo.supportsImages
                )
                loadModels()
                onModelChanged()
                Messages.showInfoMessage(
                    project,
                    "模型 '${modelInfo.alias.ifBlank { modelInfo.model }}' 添加成功",
                    "添加成功"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "添加模型失败: ${e.message}",
                    "添加失败"
                )
            }
        }
    }

    fun editModel(model: ModelConfig) {
        val dialog = EditCustomModelDialog(project, model)
        if (dialog.showAndGet()) {
            val modelInfo = dialog.getModelInfo()
            try {
                com.qifu.services.updateCustomModel(
                    oldModel = model.model,
                    newModel = modelInfo.model,
                    endpoint = modelInfo.endpoint,
                    apiKey = modelInfo.apiKey,
                    contextTokens = modelInfo.contextTokens,
                    alias = modelInfo.alias.takeIf { it.isNotBlank() },
                    supportsImages = modelInfo.supportsImages
                )
                loadModels()
                onModelChanged()
                Messages.showInfoMessage(
                    project,
                    "模型 '${modelInfo.alias.ifBlank { modelInfo.model }}' 更新成功",
                    "更新成功"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "更新模型失败: ${e.message}",
                    "更新失败"
                )
            }
        }
    }

    fun deleteModel(model: ModelConfig) {
        val result = Messages.showYesNoDialog(
            project,
            "确定要删除模型 '${model.alias}' 吗？\n\n模型名称: ${model.model}\nAPI 地址: ${model.endpoint}",
            "确认删除",
            "删除",
            "取消",
            Messages.getWarningIcon()
        )
        
        if (result == Messages.YES) {
            try {
                deleteCustomModel(model.model)
                loadModels()
                onModelChanged()
                Messages.showInfoMessage(
                    project,
                    "模型 '${model.alias}' 已成功删除",
                    "删除成功"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "删除模型失败: ${e.message}",
                    "删除失败"
                )
            }
        }
    }

    /**
     * 自定义模型表格模型
     */
    private class CustomModelsTableModel : AbstractTableModel() {
        private val models = mutableListOf<ModelConfig>()
        private val columnNames = arrayOf("模型名称", "显示名称", "API 地址", "上下文长度", "多模态", "操作")

        fun loadModels() {
            models.clear()
            models.addAll(getCustomModels())
            fireTableDataChanged()
        }

        override fun getRowCount(): Int = models.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val model = models[rowIndex]
            return when (columnIndex) {
                0 -> model.model
                1 -> model.alias
                2 -> model.endpoint
                3 -> "${model.contextTokens} tokens"
                4 -> if (model.supportsImages) "✓" else "✗"
                5 -> model // 返回整个模型对象供操作列使用
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 5 // 只有操作列可编辑
        }
    }

    /**
     * 操作按钮面板渲染器
     */
    private class ButtonPanelRenderer : JPanel(), TableCellRenderer {
        private val editButton = JButton("编辑", AllIcons.Actions.Edit).apply {
            preferredSize = Dimension(JBUI.scale(65), JBUI.scale(26))
        }
        private val deleteButton = JButton("删除", AllIcons.Actions.Cancel).apply {
            preferredSize = Dimension(JBUI.scale(65), JBUI.scale(26))
        }

        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalStrut(JBUI.scale(5)))
            add(editButton)
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(deleteButton)
            add(Box.createHorizontalStrut(JBUI.scale(5)))
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            return this
        }
    }

    /**
     * 操作按钮面板编辑器
     */
    private class ButtonPanelEditor(
        private val dialog: ManageCustomModelsDialog
    ) : AbstractCellEditor(), javax.swing.table.TableCellEditor {
        private val panel = JPanel()
        private var currentModel: ModelConfig? = null

        private val editButton = JButton("编辑", AllIcons.Actions.Edit).apply {
            preferredSize = Dimension(JBUI.scale(65), JBUI.scale(26))
            addActionListener {
                currentModel?.let { dialog.editModel(it) }
                fireEditingStopped()
            }
        }

        private val deleteButton = JButton("删除", AllIcons.Actions.Cancel).apply {
            preferredSize = Dimension(JBUI.scale(65), JBUI.scale(26))
            addActionListener {
                currentModel?.let { dialog.deleteModel(it) }
                fireEditingStopped()
            }
        }

        init {
            panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
            panel.add(Box.createHorizontalStrut(JBUI.scale(5)))
            panel.add(editButton)
            panel.add(Box.createHorizontalStrut(JBUI.scale(8)))
            panel.add(deleteButton)
            panel.add(Box.createHorizontalStrut(JBUI.scale(5)))
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            currentModel = value as? ModelConfig
            return panel
        }

        override fun getCellEditorValue(): Any? = currentModel
    }
}
