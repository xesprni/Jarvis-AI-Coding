package com.miracle.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.*

/**
 * 文件时间戳记录，跟踪文件的读取、修改和编辑状态
 *
 * @param path 文件路径
 * @param lastRead 最后读取时间戳
 * @param lastModified 最后修改时间戳
 * @param size 文件大小（字节）
 * @param lastAgentEdit Agent 最后编辑时间戳，可选
 */
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

    /** 文件新鲜度内部状态，记录读取时间戳、编辑冲突、会话文件和监控的 TODO 文件 */
    private data class FileFreshnessState(
        val readTimestamps: MutableMap<String, FileTimestamp> = mutableMapOf(), // 文件路径到时间戳记录的映射
        val editConflicts: MutableSet<String> = mutableSetOf(), // 存在编辑冲突的文件路径集合
        val sessionFiles: MutableSet<String> = mutableSetOf(), // 当前会话中访问过的文件路径集合
        val watchedTodoFiles: MutableMap<String, String> = mutableMapOf() // 正在监控的 TODO 文件路径映射
    )

    private var state = FileFreshnessState() // 当前文件新鲜度状态
    private val watchers = mutableMapOf<String, TimerTask>() // TODO 文件的定时监控任务


    /**
     * 记录文件读取操作，更新读取时间戳
     *
     * @param filePath 被读取的文件路径
     */
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
     * 文件新鲜度检查结果
     *
     * @param isFresh 文件是否新鲜（未被外部修改）
     * @param lastRead 最后读取时间戳
     * @param currentModified 当前文件修改时间戳
     * @param conflict 是否存在冲突
     */
    data class CheckFileFreshnessResult (
        val isFresh: Boolean,
        val lastRead: Long? = null,
        val currentModified: Long? = null,
        val conflict: Boolean
    )
    /**
     * 检查文件自上次读取后是否被外部修改
     *
     * @param filePath 要检查的文件路径
     * @return 文件新鲜度检查结果
     */
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

    /**
     * 记录文件编辑操作，更新修改时间戳并清除冲突标记
     *
     * @param filePath 被编辑的文件路径
     * @param content 编辑内容，当前未使用
     */
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

    /**
     * 生成文件被外部修改的提醒文本
     *
     * @param filePath 文件路径
     * @return 提醒文本，文件未变化时返回 null
     */
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

    /**
     * 获取存在编辑冲突的文件路径列表
     *
     * @return 冲突文件路径列表
     */
    fun getConflictedFiles(): List<String> = state.editConflicts.toList()

    /**
     * 获取当前会话中访问过的所有文件路径列表
     *
     * @return 会话文件路径列表
     */
    fun getSessionFiles(): List<String> = state.sessionFiles.toList()

    /**
     * 获取所有已读取文件的最后修改时间戳映射
     *
     * @return 文件路径到最后修改时间的映射
     */
    fun getReadTimestamps(): Map<String, Long> = state.readTimestamps.mapValues { it.value.lastModified }

    /**
     * 重置会话状态，清除所有监控和缓存数据
     */
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

    /**
     * 获取所有已读取文件的最后读取时间戳映射
     *
     * @return 文件路径到最后读取时间的映射
     */
    fun getFileReadTimestamps(): Map<String, Long> {
        return state.readTimestamps.mapValues { it.value.lastRead }
    }

    /**
     * 会话压缩时用于恢复的重要文件检索结果
     *
     * @param path 文件路径
     * @param timestamp 最后访问时间戳
     * @param size 文件大小（字节）
     */
    data class GetImportantFileResult(
        val path: String,
        val timestamp: Long,
        val size: Long
    )

    /**
     * 获取会话中按访问时间排序的重要文件列表，用于会话压缩时的上下文恢复
     *
     * @param maxFiles 最大返回文件数，默认为 5
     * @return 按访问时间倒序排列的重要文件列表
     */
    fun getImportantFiles(maxFiles: Int = 5): List<GetImportantFileResult> {
        return state.readTimestamps.entries
            .map { (path, info) ->
                GetImportantFileResult(path, info.lastRead, info.size)
            }
            .filter { isValidForRecovery(it.path) }
            .sortedByDescending { it.timestamp }
            .take(maxFiles)
    }

    /**
     * 判断文件路径是否适合用于上下文恢复，排除依赖、构建产物等无关文件
     *
     * @param filePath 文件路径
     * @return 是否为有效的恢复文件
     */
    private fun isValidForRecovery(filePath: String): Boolean {
        return !filePath.contains("node_modules") &&
                !filePath.contains(".git") &&
                !filePath.startsWith("/tmp") &&
                !filePath.contains(".cache") &&
                !filePath.contains("dist/") &&
                !filePath.contains("build/")
    }
}


