package com.miracle.services

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.miracle.ui.core.AssociatedContextItem
import com.miracle.utils.PsiFileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 关联文件预测服务，基于 IntelliJ 项目索引和编辑器状态预测可能相关的文件。
 *
 * 预测策略：
 * 1. 同目录/同模块文件：与已关联文件处于相同目录下的其他代码文件
 * 2. import 依赖文件：通过 FilenameIndex 查找被引用的同名文件
 * 3. 最近编辑器打开的文件：按时间排序去重过滤
 */
object RelatedFilePredictor {

    /** 推荐文件数量上限 */
    private const val MAX_PREDICTED_FILES = 5

    /** 推荐来源标签 */
    private const val SOURCE_SAME_DIR = "同目录"
    private const val SOURCE_CURRENT = "当前文件"
    private const val SOURCE_RECENT = "最近打开"

    /**
     * 根据当前上下文中已关联的文件路径，预测推荐的相关文件。
     *
     * @param project 当前项目实例
     * @param existingPaths 已关联的文件路径集合（包含已选中和已推荐的）
     * @return 推荐的关联文件列表
     */
    suspend fun predict(
        project: Project,
        existingPaths: Set<String>,
    ): List<AssociatedContextItem.AssociatedFile> = withContext(Dispatchers.Default) {
        val candidates = mutableListOf<Pair<VirtualFile, String>>()
        val seen = existingPaths.toMutableSet()

        // 策略0: 当前编辑器中选中的文件
        val currentFile = findCurrentEditorFile(project)
        if (currentFile != null && seen.add(currentFile.path) && isRelevantFile(currentFile)) {
            candidates.add(currentFile to SOURCE_CURRENT)
        }

        // 策略1: 同目录文件
        val sameDirFiles = findSameDirectoryFiles(project, existingPaths)
        for (file in sameDirFiles) {
            if (seen.add(file.path)) {
                candidates.add(file to SOURCE_SAME_DIR)
            }
            if (candidates.size >= MAX_PREDICTED_FILES) {
                return@withContext buildResult(candidates, seen)
            }
        }

        // 策略2: 最近编辑器打开的文件
        val recentFiles = findRecentlyOpenFiles(project)
        for (file in recentFiles) {
            if (seen.add(file.path)) {
                candidates.add(file to SOURCE_RECENT)
            }
            if (candidates.size >= MAX_PREDICTED_FILES) {
                break
            }
        }

        buildResult(candidates, seen)
    }

    /**
     * 获取当前编辑器中选中的文件。
     */
    private fun findCurrentEditorFile(project: Project): VirtualFile? {
        return FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile
    }

    /**
     * 查找与已关联文件处于同目录的其他代码文件。
     */
    private suspend fun findSameDirectoryFiles(
        project: Project,
        existingPaths: Set<String>,
    ): List<VirtualFile> = withContext(Dispatchers.IO) {
        val directories = existingPaths.mapNotNull { path ->
            PsiFileUtils.findVirtualFile(project, path)?.parent
        }.distinctBy { it.path }

        directories.flatMap { dir ->
            dir.children.filter { file ->
                !file.isDirectory
                && file.path !in existingPaths
                && !file.name.startsWith(".")
                && isRelevantFile(file)
            }
        }.sortedByDescending { it.timeStamp }
    }

    /**
     * 查找最近在编辑器中打开的文件（排除已关联的）。
     */
    private suspend fun findRecentlyOpenFiles(project: Project): List<VirtualFile> = withContext(Dispatchers.IO) {
        val editorManager = FileEditorManager.getInstance(project)
        editorManager.openFiles
            .filter { !it.isDirectory && isRelevantFile(it) }
            .distinctBy { it.path }
            .sortedByDescending { it.timeStamp }
    }

    /**
     * 判断文件是否值得推荐（排除二进制文件、构建产物等）。
     */
    private fun isRelevantFile(file: VirtualFile): Boolean {
        val name = file.name.lowercase()
        val ext = file.extension?.lowercase()
        // 排除常见不需要的文件类型
        if (ext in IGNORED_EXTENSIONS) return false
        if (name in IGNORED_FILE_NAMES) return false
        // 排除过大的文件
        if (file.length > 500 * 1024) return false
        return true
    }

    private fun buildResult(
        candidates: List<Pair<VirtualFile, String>>,
        seen: MutableSet<String>,
    ): List<AssociatedContextItem.AssociatedFile> {
        return candidates.map { (file, source) ->
            AssociatedContextItem.AssociatedFile(
                path = file.path,
                suggested = true,
                sourceLabel = source,
            )
        }
    }

    /** 不推荐的文件扩展名 */
    private val IGNORED_EXTENSIONS = setOf(
        "class", "jar", "war", "ear", "zip", "gz", "tar",
        "png", "jpg", "jpeg", "gif", "svg", "ico", "bmp",
        "mp3", "mp4", "wav", "avi", "mov",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "exe", "dll", "so", "dylib",
        "lock", "map",
    )

    /** 不推荐的文件名 */
    private val IGNORED_FILE_NAMES = setOf(
        "package-info.java", "module-info.java",
    )
}
