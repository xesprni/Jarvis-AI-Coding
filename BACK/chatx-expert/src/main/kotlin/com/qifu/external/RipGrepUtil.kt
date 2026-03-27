package com.qifu.external

import com.qifu.utils.CommandUtil
import com.qifu.utils.getJarvisBinDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

object RipGrepUtil {

    private const val RG_RESOURCE_PATH = "/rg/"
    private const val RG_BINARY_NAME = "rg"

    fun glob(
        glob: String, path: String?, cwd: String, searchIgnore: Boolean = true, ignoreCase: Boolean = false
    ): Flow<String> = flow {
        val cwd = if (File.separator == "/") cwd else cwd.replace("/", File.separator)
        val command = mutableListOf(
            "${getJarvisBinDirectory()}${File.separator}$RG_BINARY_NAME",
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
            "${getJarvisBinDirectory()}/$RG_BINARY_NAME",
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