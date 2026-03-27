package com.miracle.utils

import kotlinx.coroutines.flow.flow
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// ------------- ENUM & BASIC PATH SETUP -------------

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FLOW, API, STATE, REMINDER
}

private val startupTimestamp = DateTimeFormatter.ISO_INSTANT
    .format(Instant.now()).replace(":", "-").replace(".", "-")
private val requestStartTime = System.currentTimeMillis()
private val jarvisDir = File(System.getProperty("user.home"), ".jarvis")
private fun getProjectDir(): String = System.getProperty("user.dir")
    .replace(Regex("[^a-zA-Z0-9]"), "-")
private val debugBasePath = File(jarvisDir, "${getProjectDir()}/debug")
private fun detailedLogFile() = File(debugBasePath, "${startupTimestamp}-detailed.log")
private fun flowLogFile() = File(debugBasePath, "${startupTimestamp}-flow.log")
private fun apiLogFile() = File(debugBasePath, "${startupTimestamp}-api.log")
private fun stateLogFile() = File(debugBasePath, "${startupTimestamp}-state.log")

fun isDebugMode(): Boolean = System.getProperty("debug") == "true"
fun isVerboseMode(): Boolean = System.getProperty("verbose") == "true"
fun isDebugVerboseMode(): Boolean = System.getProperty("debug.verbose") == "true"

// ------------- REQUEST CONTEXT -------------

class RequestContext {
    val id: String = UUID.randomUUID().toString().take(8)
    val startTime: Long = System.currentTimeMillis()
    private val phases = mutableMapOf<String, Long>()

    fun markPhase(phase: String) {
        phases[phase] = System.currentTimeMillis() - startTime
    }

    fun getPhaseTime(phase: String): Long = phases[phase] ?: 0
    fun getAllPhases(): Map<String, Long> = phases.toMap()
}

private val activeRequests = ConcurrentHashMap<String, RequestContext>()
private var currentRequest: RequestContext? = null

fun startRequest(): RequestContext {
    val ctx = RequestContext()
    currentRequest = ctx
    activeRequests[ctx.id] = ctx
    debugLog(LogLevel.FLOW, "REQUEST_START", mapOf("requestId" to ctx.id, "activeRequests" to activeRequests.size))
    return ctx
}

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

fun getCurrentRequest(): RequestContext? = currentRequest

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

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val phase: String,
    val data: Any?,
    val requestId: String? = null,
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

object Debug {
    fun flow(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.FLOW, phase, data, requestId)
    fun api(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.API, phase, data, requestId)
    fun state(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.STATE, phase, data, requestId)
    fun info(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.INFO, phase, data, requestId)
    fun warn(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.WARN, phase, data, requestId)
    fun error(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.ERROR, phase, data, requestId)
    fun trace(phase: String, data: Any?, requestId: String? = null) = debugLog(LogLevel.TRACE, phase, data, requestId)
}