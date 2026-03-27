package com.qifu.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.*

data class FileTimestamp(
    val path: String,
    var lastRead: Long,
    var lastModified: Long,
    var size: Long,
    var lastAgentEdit: Long? = null
)

/**
 * 跟踪文件的读写、todo文件的变更
 * 提供获取重要文件的能力。
 */
class FileFreshnessService(
    private val convId: String,
    private val emitReminderEvent: (String, Any) -> Unit
) {

    private val logger = Logger.getInstance(FileFreshnessService::class.java)

    private data class FileFreshnessState(
        val readTimestamps: MutableMap<String, FileTimestamp> = mutableMapOf(),
        val editConflicts: MutableSet<String> = mutableSetOf(),
        val sessionFiles: MutableSet<String> = mutableSetOf(),
        val watchedTodoFiles: MutableMap<String, String> = mutableMapOf()
    )

    private var state = FileFreshnessState()
    private val watchers = mutableMapOf<String, TimerTask>()


    fun recordFileRead(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val timestamp = FileTimestamp(
                path = filePath,
                lastRead = System.currentTimeMillis(),
                lastModified = file.lastModified(),
                size = file.length()
            )

            state.readTimestamps[filePath] = timestamp
            state.sessionFiles.add(filePath)
        } catch (e: Exception) {
            logger.warn("Error recording file read for $filePath", e)
        }
    }

    /**
     * Check if file has been modified since last read
     */
    data class CheckFileFreshnessResult (
        val isFresh: Boolean,
        val lastRead: Long? = null,
        val currentModified: Long? = null,
        val conflict: Boolean
    )
    fun checkFileFreshness(filePath: String): CheckFileFreshnessResult {
        val recorded = state.readTimestamps[filePath] ?: return CheckFileFreshnessResult(isFresh = true, conflict = false)

        return try {
            val file = File(filePath)
            if (!file.exists()) return CheckFileFreshnessResult(isFresh = false, conflict = true)

            val currentModified = file.lastModified()
            val isFresh = currentModified <= recorded.lastModified
            val conflict = !isFresh

            if (conflict) {
                state.editConflicts.add(filePath)
                emitReminderEvent("file:conflict", mapOf(
                    "filePath" to filePath,
                    "lastRead" to recorded.lastRead,
                    "lastModified" to recorded.lastModified,
                    "currentModified" to currentModified,
                    "sizeDiff" to file.length() - recorded.size
                ))
            }

            CheckFileFreshnessResult(isFresh, recorded.lastRead, currentModified, conflict)
        } catch (e: Exception) {
            logger.warn("Error checking freshness for $filePath", e)
            CheckFileFreshnessResult(isFresh = false, conflict = true)
        }
    }

    fun recordFileEdit(filePath: String, content: String? = null) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val now = System.currentTimeMillis()
            val stats = file

            val existing = state.readTimestamps[filePath]
            if (existing != null) {
                existing.lastModified = stats.lastModified()
                existing.size = stats.length()
                existing.lastAgentEdit = now
            } else {
                state.readTimestamps[filePath] = FileTimestamp(
                    path = filePath,
                    lastRead = now,
                    lastModified = stats.lastModified(),
                    size = stats.length(),
                    lastAgentEdit = now
                )
            }

            state.editConflicts.remove(filePath)
        } catch (e: Exception) {
            logger.warn("Error recording file edit for $filePath", e)
        }
    }

    fun generateFileModificationReminder(filePath: String): String? {
        val recorded = state.readTimestamps[filePath] ?: return null

        return try {
            val file = File(filePath)
            if (!file.exists()) return "Note: $filePath was deleted since last read."

            val currentModified = file.lastModified()
            if (currentModified <= recorded.lastModified) return null

            val tolerance = 100L
            if (recorded.lastAgentEdit != null &&
                recorded.lastAgentEdit!! >= recorded.lastModified - tolerance
            ) {
                return null
            }

            "Note: $filePath was modified externally since last read. The file may have changed outside of this session."
        } catch (e: Exception) {
            logger.warn("Error checking modification for $filePath", e)
            null
        }
    }

    fun getConflictedFiles(): List<String> = state.editConflicts.toList()

    fun getSessionFiles(): List<String> = state.sessionFiles.toList()

    fun getReadTimestamps(): Map<String, Long> = state.readTimestamps.mapValues { it.value.lastModified }

    fun resetSession() {
        state.watchedTodoFiles.values.forEach { filePath ->
            try {
                watchers.remove(filePath)?.cancel()
            } catch (e: Exception) {
                logger.warn("Error unwatching file $filePath", e)
            }
        }

        state = FileFreshnessState()
    }

    fun getFileReadTimestamps(): Map<String, Long> {
        return state.readTimestamps.mapValues { it.value.lastRead }
    }

    /**
     * Retrieves files prioritized for recovery during conversation compression
     *
     * Selects recently accessed files based on:
     * - File access recency (most recent first)
     * - File type relevance (excludes dependencies, build artifacts)
     * - Development workflow importance
     *
     * Used to maintain coding context when conversation history is compressed
     */
    data class GetImportantFileResult(
        val path: String,
        val timestamp: Long,
        val size: Long
    )
    fun getImportantFiles(maxFiles: Int = 5): List<GetImportantFileResult> {
        return state.readTimestamps.entries
            .map { (path, info) ->
                GetImportantFileResult(path, info.lastRead, info.size)
            }
            .filter { isValidForRecovery(it.path) }
            .sortedByDescending { it.timestamp }
            .take(maxFiles)
    }

    private fun isValidForRecovery(filePath: String): Boolean {
        return !filePath.contains("node_modules") &&
                !filePath.contains(".git") &&
                !filePath.startsWith("/tmp") &&
                !filePath.contains(".cache") &&
                !filePath.contains("dist/") &&
                !filePath.contains("build/")
    }
}


