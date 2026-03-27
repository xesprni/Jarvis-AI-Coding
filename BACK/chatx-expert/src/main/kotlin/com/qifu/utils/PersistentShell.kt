package com.qifu.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.awaitExit
import com.qifu.utils.extensions.detectCharset
import com.qifu.utils.extensions.readTextSafely
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.random.Random


private val LOG = Logger.getInstance(PersistentShell::class.java)

// --- 数据类与常量 ---
data class ExecResult(
    val stdout: String,
    val stderr: String,
    val code: Int,
    val interrupted: Boolean
)

typealias OutputListener = (stdout: String, stderr: String) -> Unit

// DetectedShell 用于封装检测到的 shell 信息
data class DetectedShell(
    val bin: String,
    val args: List<String>,
    val type: ShellType
)

enum class ShellType { POSIX, MSYS, WSL }

// 常量
private val TEMPFILE_PREFIX = Paths.get(System.getProperty("java.io.tmpdir"), "jarvis-")
private const val DEFAULT_TIMEOUT_MS = 2 * 60 * 1000L
private const val SIGTERM_CODE = 143 // SIGTERM 的标准退出码

private object FileSuffixes {
    const val STATUS = "-status"
    const val STDOUT = "-stdout"
    const val STDERR = "-stderr"
    const val CWD = "-cwd"
}


// --- 帮助函数 ---
private fun quoteForBash(str: String): String {
    return "'${str.replace("'", "'\\''")}'"
}

fun toBashPath(pathStr: String, type: ShellType, useClassicDrive: Boolean = true): String {
    // 已经是 POSIX 绝对路径
    if (pathStr.startsWith('/')) return pathStr
    if (type == ShellType.POSIX) return pathStr

    // 规范化反斜杠
    val normalized = pathStr.replace('\\', '/')
    val driveMatch = Regex("^[A-Za-z]:").find(normalized)
    if (driveMatch != null) {
        val drive = normalized[0].lowercaseChar()
        val rest = normalized.substring(2)
        val restWithSlash = if (rest.startsWith('/')) rest else "/$rest"
        return when (type) {
            ShellType.MSYS -> if (useClassicDrive) "$drive:$restWithSlash" else "/$drive$restWithSlash"  //git bash目前可以同时支持/d/和d:/，但是/d/这种创建文件时候不太对
            ShellType.WSL -> "/mnt/$drive$restWithSlash"
            else -> normalized // 不应该到这里
        }
    }
    // 相对路径：仅转换斜杠
    return normalized
}

// 健壮的 PATH 分割器，适用于 Windows 和 POSIX
private fun splitPathEntries(pathEnv: String?): List<String> {
    if (pathEnv.isNullOrEmpty()) return emptyList()

    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    if (!isWindows) {
        return pathEnv.split(':')
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }

    // Windows: 主要分隔符是 ';', 但有些环境可能用 ':'
    // 必须避免分割驱动器号，如 'C:\' 或 'D:foo\bar'
    val entries = mutableListOf<String>()
    var current = ""
    val pushCurrent = {
        val cleaned = current.trim().removeSurrounding("\"")
        if (cleaned.isNotEmpty()) entries.add(cleaned)
        current = ""
    }

    for (ch in pathEnv) {
        when {
            ch == ';' -> pushCurrent()
            ch == ':' && !(current.length == 1 && current[0].isLetter()) -> pushCurrent()
            else -> current += ch
        }
    }
    pushCurrent() // 添加最后一个片段
    return entries
}

private fun runCommandSync(command: List<String>, timeoutMs: Long = 5000L): Boolean {
    return try {
        val process = ProcessBuilder(command).start()
        process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        process.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}

private fun detectPythonVenv(cwd: String, type: ShellType): String? {
    val cwdPath = Paths.get(cwd)
    val venvCandidates = listOf("venv", ".venv")
    
    for (venvDir in venvCandidates) {
        val venvPath = cwdPath.resolve(venvDir)
        if (!venvPath.toFile().exists()) continue
        
        val binDir = when (type) {
            ShellType.POSIX -> venvPath.resolve("bin")
            ShellType.MSYS, ShellType.WSL -> venvPath.resolve("Scripts")
        }
        
        val pythonExe = when (type) {
            ShellType.POSIX -> binDir.resolve("python")
            ShellType.MSYS, ShellType.WSL -> binDir.resolve("python.exe")
        }
        
        if (pythonExe.toFile().exists()) {
            return venvPath.toAbsolutePath().toString()
        }
    }
    
    return null
}

fun detectShell(): DetectedShell {
    val isWin = System.getProperty("os.name").lowercase().contains("win")
    if (!isWin) {
        val bin = System.getenv("SHELL") ?: "/bin/bash"
        return DetectedShell(bin, listOf("-l"), ShellType.POSIX)
    }

    // 1) 优先使用 SHELL 环境变量 (如果它指向一个存在的 bash.exe)
    System.getenv("SHELL")?.let { shellPath ->
        if (shellPath.endsWith("bash.exe", ignoreCase = true) && File(shellPath).exists()) {
            return DetectedShell(shellPath, listOf("-l"), ShellType.MSYS)
        }
    }

    // 1.1) 显式覆盖
    System.getenv("JARVIS_BASH")?.let { jarvisBashPath ->
        if (File(jarvisBashPath).exists()) {
            return DetectedShell(jarvisBashPath, listOf("-l"), ShellType.MSYS)
        }
    }

    // 2) 常见的 Git Bash/MSYS2 位置
    GitUtil.findGitBashPathFromRegistry()?.let {
        return DetectedShell(it, listOf("-l"), ShellType.MSYS)
    }
    GitUtil.findGitBashPathFromPath()?.let {
        return DetectedShell(it, listOf("-l"), ShellType.MSYS)
    }
    val programFiles = listOfNotNull(
        System.getenv("ProgramFiles"),
        System.getenv("ProgramFiles(x86)"),
        System.getenv("ProgramW6432")
    )
    val localAppData = System.getenv("LocalAppData")

    val candidates = mutableListOf<String>()
    programFiles.forEach { base ->
        candidates.add(Paths.get(base, "Git", "bin", "bash.exe").toString())
        candidates.add(Paths.get(base, "Git", "usr", "bin", "bash.exe").toString())
    }
    localAppData?.let { base ->
        candidates.add(Paths.get(base, "Programs", "Git", "bin", "bash.exe").toString())
        candidates.add(Paths.get(base, "Programs", "Git", "usr", "bin", "bash.exe").toString())
    }
    candidates.add("C:/msys64/usr/bin/bash.exe")

    candidates.firstOrNull { File(it).exists() }?.let {
        return DetectedShell(it, listOf("-l"), ShellType.MSYS)
    }

    // 2.1) 在 PATH 中搜索 bash.exe
    val pathEnv = System.getenv("PATH") ?: System.getenv("Path")
    splitPathEntries(pathEnv).forEach { entry ->
        val candidate = Paths.get(entry, "bash.exe").toString()
        if (File(candidate).exists()) {
            return DetectedShell(candidate, listOf("-l"), ShellType.MSYS)
        }
    }

    // 3) WSL
    if (runCommandSync(listOf("wsl.exe", "-e", "bash", "-lc", "echo JARVIS_OK"))) {
        return DetectedShell("wsl.exe", listOf("-e", "bash", "-l"), ShellType.WSL)
    }

    // 4) 最后手段：抛出有意义的错误
    val hint = """
        无法找到可用的 bash。请安装 Git for Windows 或启用 WSL。
        推荐安装 Git: https://git-scm.com/download/win
        或启用 WSL 并安装 Ubuntu: https://learn.microsoft.com/windows/wsl/install
    """.trimIndent()
    throw IllegalStateException(hint)
}

// --- PersistenceShell类 ---
class PersistentShell(initialCwd: String) {
    // 使用 CoroutineScope 管理所有协程的生命周期
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandMutex = Mutex() // 互斥锁，确保命令串行执行

    private val processBuilder: ProcessBuilder
    private var shellProcess: Process
    var isAlive: Boolean = true
    @Volatile private var commandInterrupted: Boolean = false
    
    // Windows Job Object for process tree management
    private var jobObject: WindowsJobObject? = null

    // 文件路径
    private val statusFile: Path
    private val stdoutFile: Path
    private val stderrFile: Path
    private val cwdFile: Path

    // bash 可见的路径
    private val statusFileBashPath: String
    private val stdoutFileBashPath: String
    private val stderrFileBashPath: String
    private val cwdFileBashPath: String

    private val binShell: String
    val shellType: ShellType
    var cwd: String = initialCwd

    init {
        val (bin, args, type) = detectShell()
        this.binShell = bin
        this.shellType = type

        processBuilder = ProcessBuilder(bin, *args.toTypedArray())
            .directory(File(initialCwd))
            .redirectErrorStream(false) // 分开处理 stdout 和 stderr

        processBuilder.environment()["GIT_EDITOR"] = "true"
        processBuilder.environment()["JARVIS_AUTH_TOKEN"] = "jarvis::${UserInfoPersistentState.getUserInfo()?.token}"
        
        if (type == ShellType.MSYS) {
            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8:replace"
        }

        this.shellProcess = processBuilder.start()
        this.isAlive = shellProcess.isAlive
        
        // 在 Windows 上创建 Job Object 并将进程加入
        if (System.getProperty("os.name").lowercase().contains("win")) {
            jobObject = WindowsJobObject.create()
            jobObject?.assignProcess(shellProcess)
        }

        scope.launch { waitExit() }

        val id = Random.nextInt(0x10000).toString(16).padStart(4, '0')
        statusFile = Paths.get(TEMPFILE_PREFIX.toString() + id + FileSuffixes.STATUS)
        stdoutFile = Paths.get(TEMPFILE_PREFIX.toString() + id + FileSuffixes.STDOUT)
        stderrFile = Paths.get(TEMPFILE_PREFIX.toString() + id + FileSuffixes.STDERR)
        cwdFile = Paths.get(TEMPFILE_PREFIX.toString() + id + FileSuffixes.CWD)

        // 初始化临时文件
        listOf(statusFile, stdoutFile, stderrFile).forEach { it.writeText("") }
        cwdFile.writeText(initialCwd)

        // 计算 bash 可见的路径
        statusFileBashPath = toBashPath(statusFile.toString(), shellType)
        stdoutFileBashPath = toBashPath(stdoutFile.toString(), shellType)
        stderrFileBashPath = toBashPath(stderrFile.toString(), shellType)
        cwdFileBashPath = toBashPath(cwdFile.toString(), shellType)

        // Source ~/.bashrc
        sendToShell("[ -f ~/.bashrc ] && source ~/.bashrc || true")
        activatePythonVenv(cwd)
    }

    fun reInit() {
        this.shellProcess = processBuilder.start()
        this.isAlive = shellProcess.isAlive
        
        // 在 Windows 上创建新的 Job Object 并将进程加入
        if (System.getProperty("os.name").lowercase().contains("win")) {
            jobObject = WindowsJobObject.create()
            jobObject?.assignProcess(shellProcess)
        }
        
        scope.launch { waitExit() }

        // 初始化临时文件
        listOf(statusFile, stdoutFile, stderrFile).forEach { it.writeText("") }
        cwdFile.writeText(cwd)

        // Source ~/.bashrc
        sendToShell("[ -f ~/.bashrc ] && source ~/.bashrc || true")
        activatePythonVenv(cwd)
    }

    private fun activatePythonVenv(cwd: String) {
        val venvPath = detectPythonVenv(cwd, shellType) ?: return

        val venvBashPath = toBashPath(venvPath, shellType, useClassicDrive = false)
        val binPath = when (shellType) {
            ShellType.POSIX -> "$venvBashPath/bin"
            ShellType.MSYS, ShellType.WSL -> "$venvBashPath/Scripts"
        }

        sendToShell("export VIRTUAL_ENV=${quoteForBash(venvBashPath)}")
        sendToShell("export PATH=${quoteForBash(binPath)}:\$PATH")
    }

    /**
     * 负责自然退出的善后
     */
    private suspend fun waitExit() {
        try {
            val exitCode = shellProcess.awaitExit()
            LOG.info("Shell process exited with code $exitCode")
        } catch (e: CancellationException) {
            LOG.debug("waitExit cancelled", e)
        } finally {
            isAlive = false
            jobObject?.close()  // 关闭 Job Object
            jobObject = null
            cleanupTempFiles()
        }

    }

    private fun cleanupTempFiles() {
        listOf(statusFile, stdoutFile, stderrFile, cwdFile).forEach {
            try {
                Files.deleteIfExists(it)
            } catch (e: Exception) {
                LOG.warn("Failed to delete temp file: $it", e)
            }
        }
    }

    /**
     * 杀掉所有的子进程
     * Windows: 使用 Job Object 机制，关闭 Job 即可杀掉所有进程树
     * Unix: 使用 descendants() 递归杀掉子进程
     */
    private suspend fun killChildProcesses() {
        if (!isAlive) return

        try {
            shellProcess.outputStream.close()
        } catch (_: Exception) {}

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        try {
            if (isWindows) {
                // Windows: 使用 Job Object 机制
                // 关闭 Job 会自动终止所有关联的进程(包括所有子进程和孙进程)
                if (jobObject != null) {
                    jobObject?.close()
                    jobObject = null
                    LOG.info("Job object closed, all child processes should be terminated")
                } else {
                    // 备用方案：如果 Job Object 不存在，使用 taskkill
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "cmd", "/c",
                            "taskkill /PID ${shellProcess.pid()} /T /F"
                        )
                    )
                }
            } else {
                // Unix: 递归杀掉所有子进程
                shellProcess.descendants().forEach {
                    try { it.destroyForcibly() } catch (_: Exception) {}
                }
                shellProcess.destroyForcibly()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to kill shell process", e)
        }

        withTimeoutOrNull(2000) {
            shellProcess.awaitExit()
        }
        commandInterrupted = true
        scope.coroutineContext.cancelChildren()
    }

    suspend fun exec(
        command: String, 
        timeout: Long = DEFAULT_TIMEOUT_MS,
        outputListener: OutputListener? = null
    ): ExecResult {
        // 使用互斥锁确保只有一个命令在执行
        return commandMutex.withLock {
            if (!isAlive) {
                reInit()
            }
            execInternal(command, timeout, outputListener)
        }
    }

    private suspend fun execInternal(command: String, timeout: Long, outputListener: OutputListener?): ExecResult {
        // 语法检查
        val checkCommand = if (shellType == ShellType.WSL) {
            listOf("wsl.exe", "-e", "bash", "-n", "-c", command)
        } else {
            listOf(binShell, "-n", "-c", command)
        }
        try {
            withTimeout(5000L) {
                val process = ProcessBuilder(checkCommand).start()
                val exitCode = process.awaitExit()
                if (exitCode != 0) {
                    val error = process.errorStream.bufferedReader().readText()
                    throw IllegalStateException(error.ifBlank { "Syntax error (exit code: $exitCode)" })
                }
            }
        } catch (e: TimeoutCancellationException) {
            return ExecResult("", e.message ?: "Syntax check failed", 128, false)
        }

        commandInterrupted = false

        stdoutFile.writeText("")
        stderrFile.writeText("")
        statusFile.writeText("")

        val quotedCommand = quoteForBash(command)
        val fullShellCommand = """
            eval $quotedCommand < /dev/null > ${quoteForBash(stdoutFileBashPath)} 2> ${quoteForBash(stderrFileBashPath)}
            EXEC_EXIT_CODE=${'$'}?
            pwd > ${quoteForBash(cwdFileBashPath)}
            echo ${'$'}EXEC_EXIT_CODE > ${quoteForBash(statusFileBashPath)}
        """.trimIndent()

        sendToShell(fullShellCommand)

        return withTimeoutOrNull(timeout) {
            var lastStdoutSize = 0L
            var lastStderrSize = 0L
            
            while (statusFile.toFile().length() == 0L && !commandInterrupted) {
                if (outputListener != null) {
                    val currentStdoutSize = stdoutFile.toFile().length()
                    val currentStderrSize = stderrFile.toFile().length()
                    
                    if (currentStdoutSize > lastStdoutSize || currentStderrSize > lastStderrSize) {
                        val stdout = if (stdoutFile.exists()) stdoutFile.readTextSafely(stdoutFile.detectCharset()) else ""
                        val stderr = if (stderrFile.exists()) stderrFile.readTextSafely(stderrFile.detectCharset()) else ""
                        outputListener(stdout, stderr)
                        
                        lastStdoutSize = currentStdoutSize
                        lastStderrSize = currentStderrSize
                    }
                }
                delay(100)
            }

            val stdout = if (stdoutFile.exists()) stdoutFile.readTextSafely(stdoutFile.detectCharset()) else ""
            val stderr = if (stderrFile.exists()) stderrFile.readTextSafely(stderrFile.detectCharset()) else ""
            val code = if (statusFile.toFile().length() > 0L) {
                statusFile.readText().trim().toIntOrNull() ?: -1
            } else {
                killChildProcesses()
                SIGTERM_CODE
            }

            ExecResult(stdout, stderr, code, commandInterrupted)

        } ?: run {
            killChildProcesses()
            val stdout = if (stdoutFile.exists()) stdoutFile.readText() else ""
            var stderr = if (stderrFile.exists()) stderrFile.readText() else ""
            stderr += (if (stderr.isNotEmpty()) "\n" else "") + "Command is still running and was stopped after reaching the configured timeout."
            ExecResult(stdout, stderr, SIGTERM_CODE, true)
        }
    }

    private fun sendToShell(command: String) {
        try {
            shellProcess.outputStream.bufferedWriter().apply {
                write(command)
                newLine()
                flush()
            }
        } catch (e: Exception) {
            LOG.warn("Error in sendToShell", e)
            throw e
        }
    }

    fun pwd(): String {
        try {
            val newCwd = cwdFile.readText().trim()
            if (newCwd.isNotEmpty()) {
                this.cwd = newCwd
            }
        } catch (e: Exception) {
            LOG.warn("Shell pwd error", e)
        }
        return this.cwd
    }

    suspend fun setCwd(newCwd: String) {
        val resolved = Paths.get(newCwd).toAbsolutePath().toString()
        if (!File(resolved).exists()) {
            throw IllegalArgumentException("Path \"$resolved\" does not exist")
        }
        val bashPath = toBashPath(resolved, this.shellType)
        exec("cd ${quoteForBash(bashPath)}")
    }

    suspend fun close() {
        killChildProcesses()
    }

}