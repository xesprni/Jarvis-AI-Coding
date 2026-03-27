package com.qifu.ui.settings.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.TopGap
import com.qifu.agent.mcp.McpInstallScope
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel

// MCP 服务安装参数描述符，用于定义需要用户输入的参数
data class McpInstallParameterDescriptor(
    val name: String,
    val description: String? = null,
)

// 对话框返回结果，包含用户选择的安装范围和参数值
data class McpInstallDialogResult(
    val scope: McpInstallScope,
    val parameterValues: Map<String, String>
)

// MCP 服务安装对话框，用于让用户选择安装范围并输入必要的参数
@Suppress("DialogTitleCapitalization")
class McpInstallDialog(
    project: Project,
    private val serviceName: String,
    private val parameterDescriptors: List<McpInstallParameterDescriptor>
) : DialogWrapper(project) {

    // 为每个安装范围创建单选按钮，默认选中全局安装
    private val scopeButtons = McpInstallScope.entries.associateWith { scope ->
        JBRadioButton(scope.displayName).apply {
            isSelected = scope == McpInstallScope.GLOBAL
        }
    }
    
    // 将单选按钮组织成互斥组，确保只能选择一个安装范围
    private val scopeButtonGroup = ButtonGroup().apply {
        scopeButtons.values.forEach { add(it) }
    }
    
    // 单选按钮的容器面板
    private val scopePanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        isOpaque = false
        scopeButtons.values.forEach { add(it) }
    }

    // 参数输入框的包装类，将描述符和实际的文本框关联起来
    private data class ParameterField(
        val descriptor: McpInstallParameterDescriptor,
        val field: JBTextField
    )

    // 根据参数描述符创建对应的输入框
    private val parameterFields = parameterDescriptors.map { descriptor ->
        ParameterField(
            descriptor,
            JBTextField().apply {
                emptyText.text = descriptor.description ?: "请输入 ${descriptor.name}"
            }
        )
    }

    // 存储对话框结果，点击 OK 后填充
    private var dialogResult: McpInstallDialogResult? = null

    init {
        title = "安装 $serviceName"
        init()
    }

    // 构建对话框的主要内容面板，使用 Kotlin UI DSL
    override fun createCenterPanel(): JComponent = panel {
        // 安装范围选择区域
        row("安装范围：") {
            cell(scopePanel)
                .align(AlignY.CENTER)
        }.layout(RowLayout.PARENT_GRID)
            .rowComment("全局在所有项目可用，项目级仅当前项目可用")

        // 根据是否有参数动态显示不同内容
        if (parameterFields.isEmpty()) {
            row {
                label("该服务无需额外参数。")
            }.topGap(TopGap.SMALL)
        } else {
            separator()
            group("额外参数") {
                parameterFields.forEach { descriptor ->
                    row("${descriptor.descriptor.name}：") {
                        cell(descriptor.field)
                            .align(FILL)
                            .resizableColumn()
                    }.layout(RowLayout.PARENT_GRID)
                        .rowComment(descriptor.descriptor.description ?: "请输入 ${descriptor.descriptor.name}")
                }
            }.topGap(TopGap.SMALL)
        }
    }.apply{
        preferredSize = Dimension(600, 400)
        minimumSize = Dimension(400, 300)
    }

    // 点击确定按钮时，收集用户输入并构造结果对象
    override fun doOKAction() {
        val scope = selectedScope()
        val params = buildMap {
            parameterFields.forEach { field ->
                put(field.descriptor.name, field.field.text.trim())
            }
        }
        dialogResult = McpInstallDialogResult(scope, params)
        super.doOKAction()
    }

    // 设置对话框打开时的默认焦点：优先聚焦到第一个参数输入框，如果没有参数则聚焦到全局安装按钮
    override fun getPreferredFocusedComponent(): JComponent? {
        return parameterFields.firstOrNull()?.field ?: scopeButtons[McpInstallScope.GLOBAL]
    }

    fun getResult(): McpInstallDialogResult? = dialogResult

    // 从按钮组中获取用户选择的安装范围
    private fun selectedScope(): McpInstallScope {
        val selectedModel = scopeButtonGroup.selection ?: return McpInstallScope.GLOBAL
        return scopeButtons.entries.firstOrNull { (_, button) -> button.model == selectedModel }?.key
            ?: McpInstallScope.GLOBAL
    }
}
