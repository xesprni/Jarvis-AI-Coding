package com.miracle.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

@Serializable
private data class CheckpointMeta(
    val userMessageId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
private data class ContextSnapshotEntry(
    val relativePath: String,
    val snapshotFile: String,
)

@Serializable
private data class ContextSnapshotManifest(
    val entries: List<ContextSnapshotEntry> = emptyList(),
)

@Serializable
private data class FileSnapshotEntry(
    val filePath: String,
    val existed: Boolean,
    val snapshotFile: String? = null,
)

@Serializable
private data class FileSnapshotManifest(
    val entries: List<FileSnapshotEntry> = emptyList(),
)

object CheckpointStorage {

    private val LOG = Logger.getInstance(CheckpointStorage::class.java)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @JvmStatic
    @Synchronized
    fun initMessageContextCheckpoint(project: Project, convId: String, userMessageId: String) {
        val messageDir = getMessageCheckpointDir(project, convId, userMessageId)
        messageDir.createDirectories()
        val contextManifestPath = getContextManifestPath(project, convId, userMessageId)
        val fileManifestPath = getFileManifestPath(project, convId, userMessageId)
        val metaPath = getMetaPath(project, convId, userMessageId)

        if (!metaPath.exists()) {
            writeJson(metaPath, CheckpointMeta(userMessageId = userMessageId))
        }
        if (contextManifestPath.exists()) {
            if (!fileManifestPath.exists()) {
                writeJson(fileManifestPath, FileSnapshotManifest())
            }
            return
        }

        val convDir = getConversationDir(project, convId)
        convDir.createDirectories()
        val contextFilesDir = getContextFilesDir(project, convId, userMessageId)
        contextFilesDir.createDirectories()

        val sourceFiles = collectContextFiles(convDir).sortedBy { it.name }
        val entries = sourceFiles.mapIndexed { index, source ->
            val snapshotName = "${index}_${source.name}"
            source.copyTo(contextFilesDir / snapshotName, overwrite = true)
            ContextSnapshotEntry(
                relativePath = source.relativeTo(convDir).pathString,
                snapshotFile = snapshotName
            )
        }

        writeJson(contextManifestPath, ContextSnapshotManifest(entries = entries))
        if (!fileManifestPath.exists()) {
            writeJson(fileManifestPath, FileSnapshotManifest())
        }
    }

    @JvmStatic
    @Synchronized
    fun recordFileCheckpointIfAbsent(project: Project, convId: String, userMessageId: String, filePath: String) {
        initMessageContextCheckpoint(project, convId, userMessageId)

        val manifestPath = getFileManifestPath(project, convId, userMessageId)
        val snapshotDir = getFileSnapshotsDir(project, convId, userMessageId)
        snapshotDir.createDirectories()

        val normalizedPath = normalizeFilePath(filePath, project)
        val manifest = readJsonOrDefault(manifestPath, FileSnapshotManifest())
        if (manifest.entries.any { it.filePath == normalizedPath }) {
            return
        }

        val targetFile = File(normalizedPath)
        val newEntry = if (targetFile.exists() && targetFile.isFile) {
            val snapshotName = "${sha256(normalizedPath)}.bin"
            val snapshotPath = snapshotDir / snapshotName
            if (!snapshotPath.exists()) {
                targetFile.toPath().copyTo(snapshotPath, overwrite = true)
            }
            FileSnapshotEntry(
                filePath = normalizedPath,
                existed = true,
                snapshotFile = snapshotName,
            )
        } else {
            FileSnapshotEntry(
                filePath = normalizedPath,
                existed = false,
                snapshotFile = null,
            )
        }

        val updatedEntries = (manifest.entries + newEntry).sortedBy { it.filePath }
        writeJson(manifestPath, FileSnapshotManifest(entries = updatedEntries))
    }

    @JvmStatic
    fun getChangedFiles(project: Project, convId: String, userMessageId: String): List<String> {
        val manifestPath = getFileManifestPath(project, convId, userMessageId)
        if (!manifestPath.exists()) {
            return emptyList()
        }
        return readJsonOrDefault(manifestPath, FileSnapshotManifest())
            .entries
            .map { it.filePath }
            .distinct()
            .sorted()
    }

    @JvmStatic
    fun hasChangedFiles(project: Project, convId: String, userMessageId: String): Boolean {
        return getChangedFiles(project, convId, userMessageId).isNotEmpty()
    }

    @JvmStatic
    fun restoreCheckpointAndContext(project: Project, convId: String, userMessageId: String): Boolean {
        return runBlocking {
            restoreCheckpointAndContextInternal(project, convId, userMessageId)
        }
    }

    private suspend fun restoreCheckpointAndContextInternal(project: Project, convId: String, userMessageId: String): Boolean {
        val messageDir = getMessageCheckpointDir(project, convId, userMessageId)
        if (!messageDir.exists()) {
            throw IllegalStateException("Checkpoint not found for message $userMessageId")
        }
        restoreFileSnapshots(project, convId, userMessageId)
        restoreConversationContext(project, convId, userMessageId)
        return true
    }

    @JvmStatic
    @Synchronized
    fun clearCheckpointsFromMessage(project: Project, convId: String, userMessageId: String) {
        val checkpointRoot = getCheckpointRoot(project, convId)
        if (!checkpointRoot.exists()) {
            return
        }

        val baseDir = checkpointRoot / userMessageId
        val baseMeta = readMeta(baseDir)
        if (baseMeta == null) {
            if (baseDir.exists()) {
                baseDir.toFile().deleteRecursively()
            }
            cleanupEmptyCheckpointRoot(checkpointRoot)
            return
        }

        val threshold = baseMeta.createdAt
        checkpointRoot.listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach { dir ->
                val meta = readMeta(dir)
                if (meta != null && meta.createdAt >= threshold) {
                    dir.toFile().deleteRecursively()
                }
            }
        cleanupEmptyCheckpointRoot(checkpointRoot)
    }

    private suspend fun restoreFileSnapshots(project: Project, convId: String, userMessageId: String) {
        val manifestPath = getFileManifestPath(project, convId, userMessageId)
        if (!manifestPath.exists()) {
            return
        }
        val snapshotsDir = getFileSnapshotsDir(project, convId, userMessageId)
        val manifest = readJsonOrDefault(manifestPath, FileSnapshotManifest())
        manifest.entries.forEach { entry ->
            val targetFile = File(entry.filePath)
            if (entry.existed) {
                val snapshotName = entry.snapshotFile
                    ?: throw IllegalStateException("Missing snapshot file name for ${entry.filePath}")
                val snapshotPath = snapshotsDir / snapshotName
                if (!snapshotPath.exists()) {
                    throw IllegalStateException("Snapshot not found: ${snapshotPath.pathString}")
                }
                targetFile.parentFile?.mkdirs()
                
                // 使用 PSI 方式更新文件,让 IDE 正确刷新 editor
                val content = snapshotPath.readText()
                val psiFile = PsiFileUtils.findPsiFile(project, entry.filePath)
                if (psiFile != null) {
                    PsiFileUtils.updatePsiFileContent(psiFile, content, flush = true)
                } else {
                    // 如果文件不存在,创建新文件
                    PsiFileUtils.createPsiFile(project, entry.filePath, content, flush = true)
                }
            } else if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    throw IllegalStateException("Failed to delete file ${targetFile.path}")
                }
            }
        }
    }

    private fun restoreConversationContext(project: Project, convId: String, userMessageId: String) {
        val convDir = getConversationDir(project, convId)
        convDir.createDirectories()
        val contextManifestPath = getContextManifestPath(project, convId, userMessageId)
        if (!contextManifestPath.exists()) {
            throw IllegalStateException("Context manifest not found for $userMessageId")
        }
        val contextFilesDir = getContextFilesDir(project, convId, userMessageId)
        val contextManifest = readJsonOrDefault(contextManifestPath, ContextSnapshotManifest())

        cleanupCurrentConversationContext(convDir)
        contextManifest.entries.forEach { entry ->
            val source = contextFilesDir / entry.snapshotFile
            if (!source.exists()) {
                throw IllegalStateException("Context snapshot not found: ${source.pathString}")
            }
            val target = convDir / entry.relativePath
            target.parent.createDirectories()
            source.copyTo(target, overwrite = true)
        }
    }

    private fun cleanupCurrentConversationContext(convDir: Path) {
        runCatching { (convDir / "chat-history.jsonl").deleteIfExists() }
            .onFailure { LOG.warn("Failed to delete chat-history snapshot target", it) }

        convDir.listDirectoryEntries("chat-memory-*.jsonl")
            .forEach { runCatching { it.deleteIfExists() }.onFailure { err -> LOG.warn("Failed to delete $it", err) } }

        convDir.listDirectoryEntries("todo-agent-*.json")
            .forEach { runCatching { it.deleteIfExists() }.onFailure { err -> LOG.warn("Failed to delete $it", err) } }
    }

    private fun collectContextFiles(convDir: Path): List<Path> {
        val files = mutableListOf<Path>()
        val historyPath = convDir / "chat-history.jsonl"
        if (historyPath.exists() && historyPath.isRegularFile()) {
            files.add(historyPath)
        }
        files.addAll(
            convDir.listDirectoryEntries("chat-memory-*.jsonl")
                .filter { it.isRegularFile() }
        )
        files.addAll(
            convDir.listDirectoryEntries("todo-agent-*.json")
                .filter { it.isRegularFile() }
        )
        return files
    }

    private fun getConversationDir(project: Project, convId: String): Path {
        return Path(getChatDirectory()) / sanitizeFileName(project.name) / convId
    }

    private fun getCheckpointRoot(project: Project, convId: String): Path {
        return getConversationDir(project, convId) / "checkpoint"
    }

    private fun getMessageCheckpointDir(project: Project, convId: String, userMessageId: String): Path {
        return getCheckpointRoot(project, convId) / userMessageId
    }

    private fun getMetaPath(project: Project, convId: String, userMessageId: String): Path {
        return getMessageCheckpointDir(project, convId, userMessageId) / "meta.json"
    }

    private fun getContextDir(project: Project, convId: String, userMessageId: String): Path {
        return getMessageCheckpointDir(project, convId, userMessageId) / "context"
    }

    private fun getContextManifestPath(project: Project, convId: String, userMessageId: String): Path {
        return getContextDir(project, convId, userMessageId) / "manifest.json"
    }

    private fun getContextFilesDir(project: Project, convId: String, userMessageId: String): Path {
        return getContextDir(project, convId, userMessageId) / "files"
    }

    private fun getFilesDir(project: Project, convId: String, userMessageId: String): Path {
        return getMessageCheckpointDir(project, convId, userMessageId) / "files"
    }

    private fun getFileManifestPath(project: Project, convId: String, userMessageId: String): Path {
        return getFilesDir(project, convId, userMessageId) / "manifest.json"
    }

    private fun getFileSnapshotsDir(project: Project, convId: String, userMessageId: String): Path {
        return getFilesDir(project, convId, userMessageId) / "snapshots"
    }

    private fun cleanupEmptyCheckpointRoot(checkpointRoot: Path) {
        if (!checkpointRoot.exists()) return
        val children = checkpointRoot.listDirectoryEntries()
        if (children.isEmpty()) {
            checkpointRoot.deleteIfExists()
        }
    }

    private fun readMeta(messageDir: Path): CheckpointMeta? {
        val metaPath = messageDir / "meta.json"
        if (!metaPath.exists()) return null
        return runCatching {
            json.decodeFromString<CheckpointMeta>(metaPath.readText())
        }.getOrElse {
            LOG.warn("Failed to parse checkpoint meta at ${metaPath.pathString}", it)
            null
        }
    }

    private fun <T> readJsonOrDefault(path: Path, defaultValue: T): T {
        if (!path.exists()) return defaultValue
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            when (defaultValue) {
                is ContextSnapshotManifest -> json.decodeFromString<ContextSnapshotManifest>(path.readText()) as T
                is FileSnapshotManifest -> json.decodeFromString<FileSnapshotManifest>(path.readText()) as T
                is CheckpointMeta -> json.decodeFromString<CheckpointMeta>(path.readText()) as T
                else -> defaultValue
            }
        }.getOrElse {
            LOG.warn("Failed to read checkpoint json at ${path.pathString}", it)
            defaultValue
        }
    }

    private fun writeJson(path: Path, data: Any) {
        path.parent.createDirectories()
        val content = when (data) {
            is ContextSnapshotManifest -> json.encodeToString(data)
            is FileSnapshotManifest -> json.encodeToString(data)
            is CheckpointMeta -> json.encodeToString(data)
            else -> throw IllegalArgumentException("Unsupported checkpoint data type: ${data::class.java.name}")
        }
        path.writeText(content)
    }

    private fun sha256(text: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
