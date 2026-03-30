package com.miracle.utils

import kotlinx.coroutines.flow.flow
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// ------------- ENUM & BASIC PATH SETUP -------------

/**
 * 日志级别枚举
 */
enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FLOW, API, STATE, REMINDER
}

/** 插件启动时间戳 */
private val startupTimestamp = DateTimeFormatter.ISO_INSTANT
    .format(Instant.now()).replace(":", "-").replace(".", "-")
/** 请求起始时间 */
private val requestStartTime = System.currentTimeMillis()
/** Jarvis 配置根目录 */
private val jarvisDir = File(System.getProperty("user.home"), ".jarvis")
/** 获取当前项目目录的标准化名称 */
private fun getProjectDir(): String = System.getProperty("user.dir")
    .replace(Regex("[^a-zA-Z0-9]"), "-")
/** 调试日志文件根路径 */
private val debugBasePath = File(jarvisDir, "${getProjectDir()}/debug")
/** 详细日志文件路径 */
private fun detailedLogFile() = File(debugBasePath, "${startupTimestamp}-detailed.log")
/** 流程日志文件路径 */
private fun flowLogFile() = File(debugBasePath, "${startupTimestamp}-flow.log")
/** API 调用日志文件路径 */
private fun apiLogFile() = File(debugBasePath, "${startupTimestamp}-api.log")
/** 状态日志文件路径 */
private fun stateLogFile() = File(debugBasePath, "${startupTimestamp}-state.log")

/** 是否启用调试模式 */
fun isDebugMode(): Boolean = System.getProperty("debug") == "true"
/** 是否启用详细模式 */
fun isVerboseMode(): Boolean = System.getProperty("verbose") == "true"
/** 是否启用调试详细模式 */
fun isDebugVerboseMode(): Boolean = System.getProperty("debug.verbose") == "true"

// ------------- REQUEST CONTEXT -------------

/**
 * 请求上下文，用于跟踪单个请求的生命周期和各阶段耗时
 */
class RequestContext {
    /** 请求唯一标识 */
    val id: String = UUID.randomUUID().toString().take(8)
    /** 请求开始时间戳 */
    val startTime: Long = System.currentTimeMillis()
    /** 各阶段耗时记录 */
    private val phases = mutableMapOf<String, Long>()

    /**
     * 标记一个阶段的时间点
     * @param phase 阶段名称
     */
    fun markPhase(phase: String) {
        phases[phase] = System.currentTimeMillis() - startTime
    }

    /**
     * 获取指定阶段的耗时
     * @param phase 阶段名称
     * @return 耗时（毫秒）
     */
    fun getPhaseTime(phase: String): Long = phases[phase] ?: 0
    /**
     * 获取所有阶段的耗时记录
     * @return 阶段名称到耗时的映射
     */
    fun getAllPhases(): Map<String, Long> = phases.toMap()
}

/** 活跃请求映射表 */
private val activeRequests = ConcurrentHashMap<String, RequestContext>()
/** 当前请求上下文 */
private var currentRequest: RequestContext? = null

/**
 * 启动一个新的请求上下文
 * @return 新创建的请求上下文
 */
fun startRequest(): RequestContext {
    val ctx = RequestContext()
    currentRequest = ctx
    activeRequests[ctx.id] = ctx
    debugLog(LogLevel.FLOW, "REQUEST_START", mapOf("requestId" to ctx.id, "activeRequests" to activeRequests.size))
    return ctx
}

/**
 * 结束指定的请求上下文
 * @param ctx 要结束的请求上下文，默认为当前请求
 */
fun endRequest(ctx: RequestContext? = currentRequest) {
    ctx?.let {
        debugLog(LogLevel.FLOW, "REQUEST_END", mapOf(
            "requestId" to it.id,
            "totalTime" to (System.currentTimeMillis() - it.startTime),
            "phases" to it.getAllPhases()
        ))
        activeRequests.remove(it.id)
        if (currentRequest?.id == it.id) currentRequest = null
    }
}

/**
 * 获取当前的请求上下文
 * @return 当前请求上下文，无活跃请求时返回 null
 */
fun getCurrentRequest(): RequestContext? = currentRequest

/**
 * 在当前请求中标记一个阶段
 * @param phase 阶段名称
 * @param phase 附加数据
 */
fun markPhase(phase: String, data: Any? = null) {
    currentRequest?.apply {
        markPhase(phase)
        Debug.flow("PHASE_${phase.uppercase()}", mapOf(
            "requestId" to id,
            "elapsed" to getPhaseTime(phase),
            "data" to data,
        ))
    }
}

// ------------- LOGGING CORE -------------

/**
 * 日志条目数据类
 */
data class LogEntry(
    /** 时间戳 */
    val timestamp: String,
    /** 日志级别 */
    val level: LogLevel,
    /** 阶段标识 */
    val phase: String,
    /** 附加数据 */
    val data: Any?,
    /** 请求 ID */
    val requestId: String? = null,
    /** 距请求开始的耗时（毫秒） */
    val elapsed: Long? = null
)

private val recentLogs = ConcurrentHashMap<String, Long>()
private const val LOG_DEDUPE_WINDOW_MS = 5000L

private fun getDedupeKey(level: LogLevel, phase: String, data: Any?): String {
    val file = (data as? Map<*, *>)?.get("file") ?: ""
    return if (phase.startsWith("CONFIG_")) "$level:$phase:$file" else "$level:$phase"
}

private fun shouldLogWithDedupe(level: LogLevel, phase: String, data: Any?): Boolean {
    val key = getDedupeKey(level, phase, data)
    val now = System.currentTimeMillis()
    val last = recentLogs[key]
    if (last == null || now - last > LOG_DEDUPE_WINDOW_MS) {
        recentLogs[key] = now
        return true
    }
    return false
}

private fun ensureDebugDir() {
    if (!debugBasePath.exists()) debugBasePath.mkdirs()
}

private fun writeToFile(file: File, entry: LogEntry) {
    if (!isDebugMode()) return
    ensureDebugDir()
    val json = entry.toJson()
    file.appendText("$json,\n")
}

private fun LogEntry.toJson(): String {
    return """{
    "timestamp": "$timestamp",
    "level": "$level",
    "phase": "$phase",
    "data": "${data.toString().take(300).replace("\"", "\\\"")}",
    "requestId": "$requestId",
    "elapsed": $elapsed
}""".trimIndent()
}

// ------------- TERMINAL OUTPUT -------------

private fun shouldShowInTerminal(level: LogLevel): Boolean {
    return if (!isDebugMode()) false
    else if (isDebugVerboseMode()) level in setOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.FLOW, LogLevel.API, LogLevel.STATE, LogLevel.INFO, LogLevel.REMINDER)
    else level in setOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.REMINDER)
}

private fun logToTerminal(entry: LogEntry) {
    if (!shouldShowInTerminal(entry.level)) return
    val time = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
    println("[$time] ${entry.level} ${entry.phase} ${entry.data?.toString()?.take(200)}")
}

// ------------- MAIN DEBUG LOGGER -------------

/**
 * 调试日志入口函数
 * @param level 日志级别
 * @param phase 阶段标识
 * @param data 附加数据
 * @param requestId 请求 ID
 */
fun debugLog(level: LogLevel, phase: String, data: Any?, requestId: String? = currentRequest?.id) {
    if (!isDebugMode()) return
    if (!shouldLogWithDedupe(level, phase, data)) return

    val entry = LogEntry(
        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date()),
        level = level,
        phase = phase,
        data = data,
        requestId = requestId,
        elapsed = currentRequest?.let { System.currentTimeMillis() - it.startTime }
    )

    writeToFile(detailedLogFile(), entry)
    when (level) {
        LogLevel.FLOW -> writeToFile(flowLogFile(), entry)
        LogLevel.API -> writeToFile(apiLogFile(), entry)
        LogLevel.STATE -> writeToFile(stateLogFile(), entry)
        else -> {}
    }
    logToTerminal(entry)
}

/**
 * 调试日志快捷方法集合
 */
object Debug {
    /** 记录流程日志 */
    fun flow(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.FLOW, phase, data, requestId)
    /** 记录 API 调用日志 */
    fun api(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.API, phase, data, requestId)
    /** 记录状态变更日志 */
    fun state(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.STATE, phase, data, requestId)
    /** 记录一般信息日志 */
    fun info(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.INFO, phase, data, requestId)
    /** 记录警告日志 */
    fun warn(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.WARN, phase, data, requestId)
    /** 记录错误日志 */
    fun error(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.ERROR, phase, data, requestId)
    /** 记录追踪日志 */
    fun trace(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.TRACE, phase, data, requestId)
}