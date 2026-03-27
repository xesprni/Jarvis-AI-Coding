package com.miracle.agent.tool

import com.intellij.openapi.project.Project
import com.miracle.agent.mcp.McpPromptIntegration
import com.miracle.config.AutoApproveSettings
import com.miracle.utils.SecurityUtil
import com.miracle.utils.normalizeFilePath

/**
 * 判断是否要允许工具执行
 * @param filePath 工具头部展示的内容： read write等工具表示的时file path，bash工具表示的待执行的指令
 */
fun shouldAutoApproveTool(toolName: String, filePath: String?, project: Project): Boolean {
    if (toolName == TodoWriteTool.getName()) return true

    val settings = AutoApproveSettings.state
    if (settings.enabled) {
        return when(toolName) {
            in setOf(
                ReadTool.getName(), GlobTool.getName(), GrepTool.getName(), ExcelReadTool.getName(),
                ReadClassTool.getName(), ResolveClassNameTool.getName(), ListImplementationsTool.getName(),
            ) -> {
                filePath?.let {
                    if (isPathInsideProject(filePath, project)) settings.actions.readFiles
                    else settings.actions.readFilesExternally
                } ?: settings.actions.readFiles
            }

            in setOf(TaskTool.getName()) -> settings.actions.runTask

            in setOf(SkillTool.getName()) -> settings.actions.runSkill

            in setOf(WriteTool.getName(), EditTool.getName()) -> {
                filePath?.let {
                    if (isPathInsideProject(filePath, project)) settings.actions.editFiles
                    else settings.actions.editFilesExternally
                } ?: settings.actions.editFiles
            }

            in setOf(BashTool.getName()) -> {
                filePath?.let {
                    if (SecurityUtil.isReadOnlyCommand(it)) settings.actions.executeSafeCommands
                    else run {
                        if (settings.actions.executeAllCommands) !SecurityUtil.isBlacklistedCommand(it) else false
                    }
                } ?: settings.actions.executeSafeCommands
            }

            //mcp 自动审批修改
            in McpPromptIntegration.toolsNameSet -> {
                settings.actions.useMcp
            }
            else -> {
                false
            }
        }
    }
    return false
}

private fun isPathInsideProject(filePath: String, project: Project): Boolean {
    if (filePath.startsWith("remote://")) return false
    if (filePath.endsWith(".class")) return true
    val cwd = project.basePath!!
    val absolutePath = normalizeFilePath(filePath, project)

    return absolutePath.startsWith(cwd)
}