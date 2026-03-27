package com.miracle.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.miracle.services.ModelConfig
import com.miracle.services.addCustomModel
import com.miracle.services.deleteCustomModel
import com.miracle.services.getCustomModels
import com.miracle.services.getSelectedModelId
import com.miracle.services.setSelectedModel
import com.miracle.services.updateCustomModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.Action
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer

class ManageCustomModelsDialog(
    private val project: Project?,
    private val onModelChanged: () -> Unit
) : DialogWrapper(project) {

    private val tableModel = CustomModelsTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowHeight = JBUI.scale(40)
        columnModel.getColumn(0).preferredWidth = JBUI.scale(150)
        columnModel.getColumn(1).preferredWidth = JBUI.scale(120)
        columnModel.getColumn(2).preferredWidth = JBUI.scale(250)
        columnModel.getColumn(3).preferredWidth = JBUI.scale(120)
        columnModel.getColumn(4).preferredWidth = JBUI.scale(80)
        columnModel.getColumn(5).preferredWidth = JBUI.scale(160)
        columnModel.getColumn(5).cellRenderer = ButtonPanelRenderer()
        columnModel.getColumn(5).cellEditor = ButtonPanelEditor(this@ManageCustomModelsDialog)
    }

    private val addButton = JButton("添加模型", AllIcons.General.Add).apply {
        addActionListener { showAddModelDialog() }
    }

    init {
        title = "管理自定义模型"
        init()
        loadModels()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout(JBUI.scale(10), JBUI.scale(10))).apply {
            preferredSize = Dimension(JBUI.scale(800), JBUI.scale(500))
            add(JBScrollPane(table), BorderLayout.CENTER)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(addButton)
                add(Box.createHorizontalGlue())
            }, BorderLayout.SOUTH)
        }
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

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
        if (!dialog.showAndGet()) {
            return
        }
        val modelInfo = dialog.getModelInfo()
        try {
            addCustomModel(
                model = modelInfo.model,
                endpoint = modelInfo.endpoint,
                apiKey = modelInfo.apiKey,
                contextTokens = modelInfo.contextTokens,
                alias = modelInfo.alias.takeIf { it.isNotBlank() },
                supportsImages = modelInfo.supportsImages
            )
            if (getSelectedModelId() == null) {
                setSelectedModel("OPENAI_COMPATIBLE_${modelInfo.model}")
            }
            loadModels()
            onModelChanged()
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "添加模型失败: ${e.message}", "添加失败")
        }
    }

    fun editModel(model: ModelConfig) {
        val dialog = EditCustomModelDialog(project, model)
        if (!dialog.showAndGet()) {
            return
        }
        val modelInfo = dialog.getModelInfo()
        try {
            updateCustomModel(
                oldModel = model.model,
                newModel = modelInfo.model,
                endpoint = modelInfo.endpoint,
                apiKey = modelInfo.apiKey,
                contextTokens = modelInfo.contextTokens,
                alias = modelInfo.alias.takeIf { it.isNotBlank() },
                supportsImages = modelInfo.supportsImages
            )
            if (getSelectedModelId() == model.id) {
                setSelectedModel("OPENAI_COMPATIBLE_${modelInfo.model}")
            }
            loadModels()
            onModelChanged()
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "更新模型失败: ${e.message}", "更新失败")
        }
    }

    fun deleteModel(model: ModelConfig) {
        val result = Messages.showYesNoDialog(
            project,
            "确定要删除模型 '${model.alias}' 吗？",
            "确认删除",
            "删除",
            "取消",
            Messages.getWarningIcon()
        )
        if (result != Messages.YES) {
            return
        }
        try {
            deleteCustomModel(model.model)
            if (getSelectedModelId() == model.id) {
                setSelectedModel(null)
            }
            loadModels()
            onModelChanged()
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "删除模型失败: ${e.message}", "删除失败")
        }
    }

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
                5 -> model
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 5
    }

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
        ): Component = this
    }

    private class ButtonPanelEditor(
        private val dialog: ManageCustomModelsDialog
    ) : javax.swing.AbstractCellEditor(), javax.swing.table.TableCellEditor {
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
