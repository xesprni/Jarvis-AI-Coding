package com.qifu.ui.settings.rules

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel
import java.io.IOException
import java.util.UUID

@Suppress("DialogTitleCapitalization")
object RulesComponent {

    private val LOG = Logger.getInstance(RulesComponent::class.java)

    @JvmStatic
    fun openRulesConfig(project: Project) {
        val basePath = project.basePath
        if (basePath.isNullOrBlank()) {
            Messages.showErrorDialog(project, "无法定位项目根目录。", "打开规则文件失败")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
            if (baseDir == null) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(project, "无法访问项目根目录：$basePath", "打开规则文件失败")
                }
                return@executeOnPooledThread
            }

            val agentsFile = findOrCreateAgentsFile(project, baseDir)
            if (agentsFile != null) {
                ApplicationManager.getApplication().invokeLater {
                    FileEditorManager.getInstance(project).openFile(agentsFile.first, true)
                    if (agentsFile.second) {
                        startRulesAuthoringSession(project, agentsFile.first, baseDir)
                    }
                }
            }
        }
    }

    private fun findOrCreateAgentsFile(project: Project, baseDir: VirtualFile): Pair<VirtualFile, Boolean>? {
        var targetFile = baseDir.findChild(AGENTS_FILE_NAME)
        if (targetFile != null) {
            return Pair(targetFile, false) // 文件已存在
        }

        var creationError: Exception? = null
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                targetFile = baseDir.createChildData(this, AGENTS_FILE_NAME)
                val apolloTemplate = ChatxApplicationSettings.settings().ruleTemplate
                val content = if (apolloTemplate.isNullOrBlank()) DEFAULT_TEMPLATE else apolloTemplate
                VfsUtil.saveText(targetFile!!, content)
            } catch (e: IOException) {
                creationError = e
            }
        }

        if (creationError != null) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(project, "创建 $AGENTS_FILE_NAME 失败：${creationError!!.message}", "打开规则文件失败")
            }
            return null
        }

        return Pair(targetFile!!, true) // 文件是新创建的
    }

    private fun startRulesAuthoringSession(project: Project, rulesFile: VirtualFile, baseDir: VirtualFile) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID)
        if (toolWindow == null) {
            LOG.warn("无法找到工具窗口，跳过自动创建规则会话。")
            return
        }

        val launchSession: () -> Unit = session@{
            val smartPanel =
                toolWindow.contentManager.contents.firstNotNullOfOrNull { it.component as? SmartToolWindowPanel }
            if (smartPanel == null) {
                LOG.warn("智能会话面板不可用，跳过自动创建规则会话。")
                return@session
            }

            val taskId = UUID.randomUUID().toString().replace("-", "")
            val tabPanel = SmartToolWindowTabPanel(project, taskId)
            val tabTitle = rulesFile.nameWithoutExtension.ifBlank { "AGENTS" }
            smartPanel.chatTabbedPane.addNewTab(tabPanel, taskId, tabTitle)
            smartPanel.chatTabbedPane.trySwitchTab(taskId)
            tabPanel.handleSubmit(buildRulesPrompt(rulesFile, baseDir))
        }

        if (toolWindow.isVisible) {
            launchSession()
        } else {
            toolWindow.activate({ launchSession() }, true)
        }
    }

    private fun buildRulesPrompt(rulesFile: VirtualFile, baseDir: VirtualFile): String {
        val relativePath = VfsUtil.getRelativePath(rulesFile, baseDir, '/')
        val location = relativePath ?: rulesFile.name
        return """
            我刚创建了项目规则文件 ${rulesFile.name}（$location），当前是默认模版。
            完善这个文件。
        """.trimIndent()
    }

    private const val AGENTS_FILE_NAME = "AGENTS.md"

    private val DEFAULT_TEMPLATE = """
        # AGENTS
        
        示例占位：
        - 项目描述：
        - 约束
        - 项目结构说明：
        - 构建与运行：
        - 单元测试说明：
        
        
    """.trimIndent()
}
