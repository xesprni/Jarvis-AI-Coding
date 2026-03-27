package com.qifu.utils

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.external.RipGrepUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 使用 ripgrep 实现的快速文件搜索服务
 * 性能远超 ProjectFileIndex.iterateContent()
 */
@Service(Service.Level.PROJECT)
class RipgrepFileSearchService(private val project: Project) {

    companion object {
        private val logger = thisLogger()
    }

    /**
     * 列出项目中的所有文件夹（包括空文件夹）
     * @param searchPattern 搜索模式,如果为空则列出所有文件夹
     * @param ignoreCase 是否忽略大小写
     * @return 匹配的文件夹列表
     */
    suspend fun listFolders(searchPattern: String = "", ignoreCase: Boolean = true): List<VirtualFile> {
        return withContext(Dispatchers.IO) {
            try {
                val projectPath = project.basePath ?: return@withContext emptyList()
                val projectFile = File(projectPath)
                
                if (!projectFile.exists() || !projectFile.isDirectory) {
                    return@withContext emptyList()
                }
                
                val lfs = LocalFileSystem.getInstance()
                val folders = mutableSetOf<VirtualFile>()
                
                // 递归遍历所有目录
                fun traverseDirectories(dir: File) {
                    if (!dir.isDirectory) return
                    
                    val virtualFile = lfs.findFileByPath(dir.absolutePath)
                    if (virtualFile != null && virtualFile.isDirectory) {
                        // 如果有搜索模式,进行过滤
                        if (searchPattern.isEmpty() || virtualFile.path.contains(searchPattern, ignoreCase = ignoreCase)) {
                            folders.add(virtualFile)
                        }
                    }
                    
                    // 递归遍历子目录
                    dir.listFiles()?.forEach { child ->
                        if (child.isDirectory) {
                            traverseDirectories(child)
                        }
                    }
                }
                
                // 添加项目根目录
                val rootVirtualFile = lfs.findFileByPath(projectPath)
                if (rootVirtualFile != null && (searchPattern.isEmpty() || 
                    rootVirtualFile.path.contains(searchPattern, ignoreCase = ignoreCase))) {
                    folders.add(rootVirtualFile)
                }
                
                // 遍历所有子目录
                projectFile.listFiles()?.forEach { child ->
                    if (child.isDirectory) {
                        traverseDirectories(child)
                    }
                }
                
                folders.toList().also {
                    logger.debug("Found ${it.size} folders for pattern: $searchPattern")
                }
            } catch (e: Exception) {
                logger.warn("Error listing folders", e)
                emptyList()
            }
        }
    }

    /**
     * 使用模糊匹配搜索文件
     * @param pattern 搜索模式(文件名的一部分)
     * @param ignoreCase 是否忽略大小写
     * @param maxResults 最大返回结果数
     * @return 匹配的文件列表
     */
    suspend fun searchFiles(
        pattern: String,
        ignoreCase: Boolean = true,
        maxResults: Int = 100
    ): List<VirtualFile> {
        return withContext(Dispatchers.IO) {
            try {
                val projectPath = project.basePath ?: return@withContext emptyList()
                
                // 使用ripgrep的文件名匹配
                val glob = "*${pattern}*"
                
                val filePaths = RipGrepUtil.glob(
                    glob = glob,
                    path = null,
                    cwd = projectPath,
                    searchIgnore = false,
                    ignoreCase = ignoreCase
                ).toList().take(maxResults)
                
                // 转换为VirtualFile
                val lfs = LocalFileSystem.getInstance()
                filePaths.mapNotNull { relativePath ->
                    val absolutePath = File(projectPath, relativePath).absolutePath
                    lfs.findFileByPath(absolutePath)?.takeIf { !it.isDirectory }
                }.also {
                    logger.debug("ripgrep search found ${it.size} files for pattern: $pattern")
                }
            } catch (e: Exception) {
                logger.warn("Error searching files with ripgrep", e)
                emptyList()
            }
        }
    }

    /**
     * 搜索文件内容(grep功能)
     * @param contentPattern 内容搜索模式
     * @param filePattern 文件名模式(可选)
     * @param ignoreCase 是否忽略大小写
     * @return 包含匹配内容的文件列表
     */
    suspend fun searchFileContent(
        contentPattern: String,
        filePattern: String? = null,
        ignoreCase: Boolean = true
    ): List<VirtualFile> {
        return withContext(Dispatchers.IO) {
            try {
                val projectPath = project.basePath ?: return@withContext emptyList()
                
                val filePaths = RipGrepUtil.grep(
                    pattern = contentPattern,
                    path = null,
                    cwd = projectPath,
                    glob = filePattern,
                    outputMode = "files_with_matches",
                    caseInsensitive = ignoreCase
                ).toList()
                
                // 转换为VirtualFile
                val lfs = LocalFileSystem.getInstance()
                filePaths.mapNotNull { relativePath ->
                    val absolutePath = File(projectPath, relativePath).absolutePath
                    lfs.findFileByPath(absolutePath)
                }
            } catch (e: Exception) {
                logger.warn("Error searching file content with ripgrep", e)
                emptyList()
            }
        }
    }
}
