package com.miracle.utils

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager
import com.miracle.agent.tool.ToolParameterException
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.pathString

private val LOG = Logger.getInstance("com.miracle.utils.config")
/**
 * 获取当前活动的project
 */
fun getCurrentProject(): Project? {
    return try {
        // 优先获取最后聚焦窗口的 Project
        val frame = IdeFocusManager.getGlobalInstance().lastFocusedFrame
        val project = frame?.project
        if (project != null && !project.isDisposed) return project

        // 遍历已打开的 Project
        for (p in ProjectManager.getInstance().openProjects) {
            if (!p.isDisposed) return p
        }

        // fallback 默认 Project
        ProjectManager.getInstance().defaultProject
    } catch (e: Exception) {
        null
    }
}

/**
 * 获取当前 Project 根目录路径
 */
fun getCurrentProjectRootPath(): String {
    val project = getCurrentProject()
    return project?.basePath ?: System.getProperty("user.dir")
}

/**
 * 验证文件路径是否在项目根目录或其子目录内
 * @param filePath 需要验证的文件路径
 * @throws ToolParameterException 当路径在项目目录外时抛出异常
 */
fun isFilePathInProject(filePath: String, project: Project): Boolean {
    val projectRoot = project.basePath!!
    val fullPath = normalizeFilePath(filePath, project)
    val isInProject = fullPath.startsWith(projectRoot)
    if (!isInProject) {
        LOG.warn("File path $filePath is not in project root $projectRoot")
    }
    return isInProject
}

/**
 * Get the jarvis config directory
 */
fun getUserConfigDirectory(): String {
    val configDir = System.getenv("JARVIS_CONFIG_DIR")
        ?: run {
            val home = System.getProperty("user.home")
            return "$home${File.separator}.jarvis"
        }
    File(configDir).mkdirs()
    return configDir
}

/**
 * 获取项目级配置目录路径
 * @param project 当前项目，为 null 时使用当前活跃项目
 * @return 项目配置目录路径
 */
fun getProjectConfigDirectory(project: Project? = null): String {
    val basePath = project?.basePath ?: getCurrentProjectRootPath()
    val configDir = "$basePath${File.separator}.jarvis"
    File(configDir).mkdirs()
    return configDir
}

/**
 * 获取插件二进制文件目录路径
 * @return bin 目录路径
 */
fun getJarvisBinDirectory(): String {
    val jarvisBinDir = "${PathManager.getPluginsPath()}${File.separator}chatx-expert${File.separator}bin"
    File(jarvisBinDir).mkdirs()
    return jarvisBinDir
}

/**
 * 获取聊天记录存储目录路径
 * @return 聊天记录目录路径
 */
fun getChatDirectory(): String {
    val configDir = Path(getUserConfigDirectory()) / "intellij-chat-v2"
    configDir.createDirectories()
    return configDir.pathString
}

/**
 * 获取计划存储目录路径
 * @param project 当前项目，为 null 时使用当前活跃项目
 * @param convId 会话 ID，为 null 时使用 "default"
 * @return 计划存储目录路径
 */
fun getPlanDirectory(project: Project? = null, convId: String? = null): String {
    val projectName = project?.name ?: "default-project"
    val planDir = Path(getChatDirectory()) / sanitizeFileName(projectName) / (convId ?: "default")
    planDir.createDirectories()
    return planDir.pathString
}

/** MCP 配置子目录名 */
const val MCP_CONFIG_DIRECTORY = "mcp"
