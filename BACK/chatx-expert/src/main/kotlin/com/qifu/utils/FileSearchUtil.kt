package com.qifu.utils

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.PlatformUtils

object FileSearchUtil {

    /**
     * 智能搜索文件：
     *  - "MyFile.java" → 精确匹配（FilenameIndex）
     *  - "*.java" → 按扩展名（FilenameIndex）
     *  - "src/**/Test*.kt" → glob 匹配（遍历 VFS）
     *  - "如果是.class 结尾则查找直接查找依赖lib"
     */
    @JvmStatic
    suspend fun globFiles(
        project: Project,
        pattern: String,
        dirPath: String? = null,
        maxResults: Int = Int.MAX_VALUE,
    ): List<VirtualFile> {
        val files = when {
            isExactFilename(pattern) -> searchByExactFilename(project, pattern, dirPath)
            isExtensionPattern(pattern) -> searchByExtension(project, extractExtension(pattern), dirPath)
            else -> searchByGlobPattern(project, pattern, dirPath, maxResults)
        }
        return files.takeIf { it.isNotEmpty() } ?: findClassWithAllScope(project, pattern)
    }

    // -------------------------------
    // 精确文件名匹配
    // -------------------------------
    private fun isExactFilename(pattern: String): Boolean {
        val parts = pattern.split('/', '\\')
        if (parts.isEmpty()) return false
        // 检查除了最后一部分外，其他部分是否都是 "*" 或 "**"
        val isPrefixValid = parts.dropLast(1).all { it == "*" || it == "**" }
        // 检查最后一部分是否不包含通配符
        val isLastNameValid = !parts.last().contains('*') && !parts.last().contains('?')
        return isPrefixValid && isLastNameValid
    }

    private suspend fun searchByExactFilename(project: Project, filename: String, dirPath: String?): List<VirtualFile> {
        val scope = createScope(project, dirPath)
        val baseName = filename.split('/', '\\').last()
        return readAction {
            FilenameIndex.getVirtualFilesByName(baseName, false, scope).toList().sortedByDescending { it.timeStamp }
        }
    }

    // -------------------------------
    // 扩展名匹配 (*.java / *.kt)
    // -------------------------------
    private fun isExtensionPattern(pattern: String): Boolean {
        return pattern.startsWith("*.") && !pattern.contains('/') && !pattern.contains('?')
    }

    //判断是否以.class结尾
//    private fun isClassExtensionPattern(pattern: String): Boolean {
//        return pattern.endsWith(".class")
//    }
//
//    private suspend fun searchByClassFilename(project: Project, filename: String): List<VirtualFile> {
//        val cache = PsiShortNamesCache.getInstance(project)
//        val allScope = GlobalSearchScope.allScope(project)
//        //将filename的特殊字符全部去掉
//        val classes = cache.getClassesByName(cleanName(filename), allScope)
//        return classes.mapNotNull { psiClass ->
//            psiClass.containingFile?.virtualFile
//        }.distinct()
//    }

    fun cleanName(raw: String): String {
        val noClassSuffix = raw.removeSuffix(".class")
        // 去掉特殊字符，只保留字母、数字和下划线
        //    [^A-Za-z0-9_] 表示“不是字母数字下划线的全部替换掉”
        return noClassSuffix.replace(Regex("[^A-Za-z0-9_]"), "")
    }

    suspend fun findClassWithAllScope(
        project: Project,
        pattern: String,
    ): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        // 如果不是 IDEA，直接返回空结果
        if (!PlatformUtils.isIntelliJ()) {
            return result
        }
        
        readAction {
            try { // 去掉后缀
                val noSuffix = pattern.substringBeforeLast(".", pattern)
                val nameRegex = cleanName(noSuffix)
                val cache = PsiShortNamesCache.getInstance(project)
                // 从索引拿所有短类名
                val allClassNames = cache.allClassNames
                val matchedNames = allClassNames.filter { shortName ->
                    shortName.startsWith(nameRegex, ignoreCase = true)
                }
                // 再按 scope 查 PsiClass
                val scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
                val psiClasses = matchedNames.flatMap { shortName ->
                    cache.getClassesByName(shortName, scope).asList()
                }
                // 转 VirtualFile 列表并去重
                result.addAll(psiClasses.mapNotNull { PsiUtilCore.getVirtualFile(it) }.distinct())
            } catch (ignore: Exception) {
                //忽略异常情况 比如在pycharm中调用
            }
        }
        return result
    }

    private fun extractExtension(pattern: String): String {
        return pattern.removePrefix("*.") // "*.java" → "java"
    }

    private suspend fun searchByExtension(project: Project, ext: String, dirPath: String?): List<VirtualFile> {
        val scope = createScope(project, dirPath)
        return readAction {
             FilenameIndex.getAllFilesByExt(project, ext, scope).toList().sortedByDescending { it.timeStamp }
        }
    }

    // -------------------------------
    // 通用 Glob 匹配 (**/*.java 等)
    // -------------------------------
    private suspend fun searchByGlobPattern(project: Project, pattern: String, dirPath: String?, maxResults: Int): List<VirtualFile> {
        val basePath = dirPath ?: project.basePath!!
        val baseDir = PsiFileUtils.findVirtualFile(project, basePath) ?: return emptyList()

        val regex = globToRegex(pattern)
        val matched = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(
            baseDir,
            null,
            ContentIterator { file ->
                if (matched.size >= maxResults) return@ContentIterator false
                if (!file.isDirectory) {
                    val relPath = VfsUtilCore.getRelativePath(file, baseDir, '/') ?: return@ContentIterator true
                    if (regex.matches(relPath)) {
                        matched.add(file)
                    }
                }
                true // 返回 true 继续遍历
            }
        )
        return matched.sortedByDescending { it.timeStamp }
    }

    // -------------------------------
    // Scope 工具
    // -------------------------------
    private suspend fun createScope(project: Project, dirPath: String?): GlobalSearchScope {
        if (dirPath.isNullOrEmpty()) return GlobalSearchScope.projectScope(project)

        val vFile = PsiFileUtils.findVirtualFile(project, dirPath)
        return if (vFile != null) {
            GlobalSearchScopesCore.DirectoryScope(project, vFile, true)
        } else {
            GlobalSearchScope.projectScope(project)
        }
    }

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        var i = 0
        while (i < glob.length) {
            when (val c = glob[i]) {
                '*' -> {
                    if (i + 1 < glob.length && glob[i + 1] == '*') {
                        // 匹配任意层目录
                        sb.append(".*")
                        i++ // 跳过下一个 *
                    } else {
                        // 匹配任意字符（不含 /）
                        sb.append("[^/]*")
                    }
                }
                '?' -> sb.append(".")
                '.', '(', ')', '+', '|', '^', '$', '@', '%' -> sb.append("\\$c")
                '\\' -> sb.append("\\\\")
                else -> sb.append(c)
            }
            i++
        }
        sb.append("$")
        return Regex(sb.toString())
    }
}