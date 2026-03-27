package com.miracle.ui.settings.rules

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import com.miracle.config.JarvisCoreSettings

@Suppress("DialogTitleCapitalization")
object RulesComponent {

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
                val template = JarvisCoreSettings.getInstance().ruleTemplate
                val content = if (template.isBlank()) DEFAULT_TEMPLATE else template
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
