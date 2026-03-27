package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.qifu.services.ModelConfig
import com.qifu.services.loadModelConfigs
import com.qifu.ui.smartconversation.settings.service.ModelChangeNotifier
import com.qifu.ui.smartconversation.settings.service.ModelChangeNotifierAdapter
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.Color
import javax.swing.JComponent
import javax.swing.SwingUtilities


/**
 * @author weiyichao
 * @date 2025-09-26
 **/
class ModelComboBoxAction(
    private val project: Project,
    private val onModelChange: (String, Boolean) -> Unit,
) : ComboBoxAction() {

    @Volatile
    private var models: Map<String, List<ModelConfig>> = emptyMap()

    @Volatile
    private var modelList: List<ModelConfig> = emptyList()


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private val LOG = Logger.getInstance(ModelComboBoxAction::class.java)
    }

    private var modelConfig: ModelConfig? = null


    init {
        isSmallVariant = true
        loadModelsAsync()
        subscribeModelChanges()
    }

    fun getCurrentModelId(): String {
        return modelConfig?.id ?: "JARVIS_360/qwen3-coder-480b-a35b"
    }


    fun createCustomComponent(place: String): JComponent {
        return createCustomComponent(templatePresentation, place)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = createComboBoxButton(presentation)
        button.foreground = JBColor.lazy {
            EditorColorsManager.getInstance().globalScheme.defaultForeground
        }
        button.border = null
        button.putClientProperty("JButton.backgroundColor", Color(0, 0, 0, 0))
        return button
    }

    // 异步加载模型配置并更新UI
    private fun loadModelsAsync() {
        scope.launch {
            try {
                loadModelConfigs(forceUpdate = true).takeIf { it.isNotEmpty() }?.let { configs ->
                    modelList = configs.values.toList()
                    ChatxApplicationSettings.settings().chatModelId?.let { lastModelId ->
                        modelList.firstOrNull() {
                            it.id == lastModelId
                        }?.let { modelConfig = it }
                    }
                    models = modelList.groupBy { it.provider.desc }
                    with(Dispatchers.EDT) {
                        updateTemplatePresentation()
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Failed to load models", e)
            }
        }
    }

    private fun subscribeModelChanges() {
        val messageBus = ApplicationManager.getApplication().messageBus.connect()
        messageBus.subscribe(ModelChangeNotifier.getTopic(), object : ModelChangeNotifierAdapter() {
            override fun chatModelChanged(
                newModel: String,
                serviceType: com.qihoo.finance.lowcode.smartconversation.service.ServiceType
            ) {
                SwingUtilities.invokeLater {
                    updateTemplatePresentation()
                }
            }
        })
    }

    private fun updateTemplatePresentation() {
        updateTemplatePresentation(getCurrentModelId())
    }

    // 更新下拉框的显示内容和保存模型配置：
    private fun updateTemplatePresentation(newModelId: String) {
        val templatePresentation = templatePresentation
        var model = modelList.find { model -> model.id == newModelId }
        if (model == null && modelList.isNotEmpty()) {
            model = modelList.first()
        }
        if (model != null) {
            templatePresentation.icon = model.icon
            // 是否展示限量
            templatePresentation.text = getModelDisplayName(model, showLimitTag = false)
            modelConfig = model
            ChatxApplicationSettings.settings().chatModelId = newModelId
            ChatxApplicationSettings.settings().modelSupportImage = model.supportsImages
            onModelChange(newModelId, model.supportsImages)
        }
    }

    // 点击button展示模型列表
    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        models.forEach { (provider, configs) ->
            actionGroup.addSeparator(provider)
            configs.forEach { model ->
                val action = object : AnAction(getModelDisplayName(model), null, model.icon) {
                    override fun actionPerformed(e: AnActionEvent) {
                        onModelSelected(model)
                    }
                }
                actionGroup.add(action)
            }
        }

        // 添加"添加模型"和"管理模型"按钮
        actionGroup.addSeparator()
        val addModelAction = object : AnAction("添加自定义模型...", "添加自定义 OpenAI 兼容模型", com.intellij.icons.AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                showAddModelDialog()
            }
        }
        actionGroup.add(addModelAction)
        
        val manageModelAction = object : AnAction("管理自定义模型...", "管理和删除自定义模型", com.intellij.icons.AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                showManageModelsDialog()
            }
        }
        actionGroup.add(manageModelAction)

        return actionGroup
    }

    private fun getModelDisplayName(model: ModelConfig, showLimitTag: Boolean = true): String {
        return if (model.isLimit && showLimitTag) {
            """<html>${model.alias} <span color='#D4A944'>[限量]</span></html>"""
        } else {
            model.alias
        }
    }

    override fun shouldShowDisabledActions() = true

    private fun onModelSelected(model: ModelConfig) {
        updateTemplatePresentation(model.id)
    }

    private fun showAddModelDialog() {
        val dialog = AddCustomModelDialog(project)
        if (dialog.showAndGet()) {
            val modelInfo = dialog.getModelInfo()
            scope.launch {
                try {
                    com.qifu.services.addCustomModel(
                        model = modelInfo.model,
                        endpoint = modelInfo.endpoint,
                        apiKey = modelInfo.apiKey,
                        contextTokens = modelInfo.contextTokens,
                        alias = modelInfo.alias.takeIf { it.isNotBlank() }
                    )
                    // 重新加载模型列表
                    loadModelsAsync()
                    with(Dispatchers.EDT) {
                        com.intellij.openapi.ui.Messages.showInfoMessage(
                            project,
                            "模型 ${modelInfo.alias.ifBlank { modelInfo.model }} 添加成功",
                            "添加成功"
                        )
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to add custom model", e)
                    with(Dispatchers.EDT) {
                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            project,
                            "添加模型失败: ${e.message}",
                            "添加失败"
                        )
                    }
                }
            }
        }
    }

    private fun showManageModelsDialog() {
        val dialog = ManageCustomModelsDialog(project) {
            // 删除成功后的回调：重新加载模型列表
            loadModelsAsync()
        }
        dialog.show()
    }

}
