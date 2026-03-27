package com.miracle.external

import com.intellij.openapi.diagnostic.thisLogger
import com.miracle.utils.CommandUtil
import com.miracle.utils.getJarvisBinDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

object RipGrepUtil {

    private const val RG_RESOURCE_PATH = "/rg/"
    private const val RG_BINARY_NAME = "rg"
    private val LOG = thisLogger()
    @Volatile
    private var installVerified = false

    fun glob(
        glob: String, path: String?, cwd: String, searchIgnore: Boolean = true, ignoreCase: Boolean = false
    ): Flow<String> = flow {
        val cwd = if (File.separator == "/") cwd else cwd.replace("/", File.separator)
        val command = mutableListOf(
            resolveBinaryPath(),
            "--files",
            "--hidden",
            "--sort=modified",
        )
        if (searchIgnore) command.add("--no-ignore")
        if (ignoreCase) command.add("--glob-case-insensitive")
        if (glob.isNotBlank()) {
            for (globPart in parseGlobString(glob)) {
                command.add("--glob")
                command.add(globPart)
            }
        }
        if (!path.isNullOrBlank()) {
            Path(path).normalize().pathString.takeIf { it.isNotBlank() }?.let { command.add(it) }
        }
        emitAll(CommandUtil.run(command, cwd))
    }

    fun grep(
        pattern: String?, path: String?, cwd: String, glob: String? = null, type: String? = null,
        outputMode: String = "files_with_matches", before: Int? = null, after: Int? = null, context: Int? = null,
        lineNumbers: Boolean = true, caseInsensitive: Boolean = false, multiline: Boolean = false,
    ): Flow<String> = flow {
        val cwd = if (File.separator == "/") cwd else cwd.replace("/", File.separator)
        val command = mutableListOf(
            resolveBinaryPath(),
            "--heading",
            "--hidden",
            "--max-columns", "500",
        )
        if (!glob.isNullOrBlank()) {
            for (globPart in parseGlobString(glob)) {
                command.add("--glob")
                command.add(globPart)
            }
        }
        if (!type.isNullOrBlank()) command.addAll(listOf("--type", type))
        if (lineNumbers) command.add("-n")
        if (caseInsensitive) command.add("-i")
        if (multiline) command.addAll(listOf("-U", "--multiline-dotall"))
        when (outputMode) {
            "files_with_matches" -> command.add("-l")
            "count" -> command.add("-c")
            else -> {
                context?.let { command.addAll(listOf("-C", it.toString())) } ?: run {
                    before?.let { command.addAll(listOf("-B", it.toString())) }
                    after?.let { command.addAll(listOf("-A", it.toString())) }
                }
            }
        }
        pattern?.let {
            if (it.startsWith("-")) command.addAll(listOf("-e", pattern)) else command.add(pattern)
        }
        path?.takeIf { it.isNotBlank() }
            ?.let { Path(it).normalize().pathString }
            ?.takeIf { it.isNotBlank() }
            ?.let { command.add(it) }
            ?: run { command.add(cwd) }

        emitAll(CommandUtil.run(command, cwd))
    }

    @JvmStatic
    fun install() {
        CommandUtil.installFromJar(RG_RESOURCE_PATH, RG_BINARY_NAME, getJarvisBinDirectory())
    }

    @JvmStatic
    fun ensureInstalled() {
        val targetFile = File(getJarvisBinDirectory(), currentBinaryName())
        if (installVerified && targetFile.exists()) {
            ensureExecutable(targetFile)
            return
        }

        synchronized(this) {
            if (targetFile.exists()) {
                ensureExecutable(targetFile)
                installVerified = true
                return
            }

            runCatching { install() }
                .onSuccess { installVerified = true }
                .onFailure { LOG.warn("Failed to prepare ripgrep binary from bundled resources", it) }

            if (targetFile.exists()) {
                ensureExecutable(targetFile)
                installVerified = true
            }
        }
    }

    private fun resolveBinaryPath(): String {
        ensureInstalled()
        val targetFile = File(getJarvisBinDirectory(), currentBinaryName())
        return if (targetFile.exists()) {
            targetFile.absolutePath
        } else {
            LOG.warn("Bundled ripgrep binary is unavailable, falling back to system rg")
            if (File.separatorChar == '\\') "rg.exe" else RG_BINARY_NAME
        }
    }

    private fun currentBinaryName(): String {
        return if (CommandUtil.getOSInfo().isWindows) "$RG_BINARY_NAME.exe" else RG_BINARY_NAME
    }

    private fun ensureExecutable(file: File) {
        if (!file.canExecute()) {
            file.setExecutable(true, false)
        }
    }

    private fun parseGlobString(glob: String): List<String> {
        // rg 不支持空格，逗号分隔的glob表达式，需要进行一些转换：
        // "*.ts,*.js !dist/** *.{kt,kts}" -> ["*.ts", "*.js", "!dist/**", "*.{kt,kts}"]
        val parts = glob.split(Regex("\\s+")).filter { it.isNotBlank() }
        val expanded = mutableListOf<String>()

        for (part in parts) {
            if (part.contains('{') && part.contains('}')) {
                expanded.add(part)
                continue
            }
            expanded.addAll(
                part.split(',')
                    .filter { it.isNotBlank() }
            )
        }

        return expanded
    }

}
