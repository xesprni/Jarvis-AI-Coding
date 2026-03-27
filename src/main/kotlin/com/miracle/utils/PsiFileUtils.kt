package com.miracle.utils

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Paths


object PsiFileUtils {

    /**
     * 根据文件路径获取 PsiFile
     */
    suspend fun findPsiFile(project: Project, filePath: String): PsiFile? {
        val fullPath = normalizeFilePath(filePath, project)
        val vFile = LocalFileSystem.getInstance().findFileByPath(fullPath) ?:
            LocalFileSystem.getInstance().refreshAndFindFileByPath(fullPath)
        return readAction {
            vFile?.let {
                PsiManager.getInstance(project).findFile(it)
            }
        }
    }

    suspend fun findVirtualFile(project: Project, filePath: String): VirtualFile? {
        val fullPath = normalizeFilePath(filePath, project)
        return readAction {
            LocalFileSystem.getInstance().findFileByPath(fullPath)
        }
    }

    /**
     * 获取 PsiFile 对应的 Document
     */
    suspend fun getDocument(psiFile: PsiFile): Document? {
        return readAction {
            PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
        }
    }

    /**
     * 读取文件内容
     */
    suspend fun getFileContent(project: Project, filePath: String): String? {
        return if (filePath.endsWith(".class", ignoreCase = true)) {
            val resolvedPath = preparePath(filePath, project)
            val vFile = findVirtualFileInternal(resolvedPath) ?: return null
            readAction {
                val findFile = PsiManager.getInstance(project).findFile(vFile)
                //如果有源码 则直接读取源码给模型
                val sourceFile = findFile?.navigationElement
                if (sourceFile is PsiFile && sourceFile != findFile) {
                    return@readAction sourceFile.text
                }
                findFile?.text
            }
        } else {
            findPsiFile(project = project, filePath = filePath)?.let {
                getDocument(it)?.text
            }
        }
    }

    /**
     * 更新 PsiFile 内容
     */
    suspend fun updatePsiFileContent(psiFile: PsiFile, content: String, flush:Boolean = false): String? {
        return getDocument(psiFile)?.let { document ->
            val oldContent = document.text
            // 使用 WriteCommandAction 来支持撤销功能
            WriteCommandAction.runWriteCommandAction(psiFile.project) {
                document.setText(content)
                PsiDocumentManager.getInstance(psiFile.project).commitDocument(document)
                if (flush) FileDocumentManager.getInstance().saveDocument(document)
            }
            oldContent
        }
    }

    suspend fun createPsiFile(project: Project, filePath: String, content: String = "", flush: Boolean = false): PsiFile? {
        val fullPath = Paths.get(normalizeFilePath(filePath, project))
        val parentPath = fullPath.parent?.toString() ?: return null
        val fileName = fullPath.fileName.toString()

        val psiFile = writeAction {
            // 获取或创建文件
            val parentVirtualFile = LocalFileSystem.getInstance().findFileByPath(parentPath)
                ?: VfsUtil.createDirectoryIfMissing(parentPath)
            val existingFile = parentVirtualFile!!.findChild(fileName)
            val virtualFile = existingFile ?: parentVirtualFile.createChildData(this, fileName)

            // 写入内容
            VfsUtil.saveText(virtualFile, content)

            // 返回 PsiFile
            PsiManager.getInstance(project).findFile(virtualFile)
        }
        if (flush) {
            val document = getDocument(psiFile!!)
            flushDocumentToFile(document!!)
        }
        return psiFile
    }

    suspend fun flushDocumentToFile(document: Document) {
        writeAction {
            FileDocumentManager.getInstance().saveDocument(document)
        }
    }

    private fun preparePath(filePath: String, project: Project): String {
        return if (filePath.contains("://")) {
            toPosixPath(filePath)
        } else {
            normalizeFilePath(filePath, project)
        }
    }

    private fun findVirtualFileInternal(filePath: String): VirtualFile? {
        val normalizedPath = toPosixPath(filePath)
        val virtualFileManager = VirtualFileManager.getInstance()

        if (normalizedPath.contains("://")) {
            virtualFileManager.findFileByUrl(normalizedPath)?.let { return it }
        }

        val localFileSystem = LocalFileSystem.getInstance()
        localFileSystem.findFileByPath(normalizedPath)?.let { return it }
        localFileSystem.refreshAndFindFileByPath(normalizedPath)?.let { return it }

        if (normalizedPath.contains("!/")) {
            val jarFileSystem = JarFileSystem.getInstance()
            val jarPath = normalizedPath.removePrefix("jar://")
            jarFileSystem.findFileByPath(jarPath)?.let { return it }
            jarFileSystem.refreshAndFindFileByPath(jarPath)?.let { return it }
            if (!normalizedPath.startsWith("jar://")) {
                virtualFileManager.findFileByUrl("jar://$jarPath")?.let { return it }
            }
        }

        return null
    }
}
