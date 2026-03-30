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

/**
 * RipGrep 命令行工具封装，提供文件搜索和内容搜索功能
 */
object RipGrepUtil {

    /** 资源文件中的 rg 目录路径 */
    private const val RG_RESOURCE_PATH = "/rg/"
    /** rg 二进制文件名称 */
    private const val RG_BINARY_NAME = "rg"
    /** 日志记录器 */
    private val LOG = thisLogger()
    /** 安装验证标记 */
    @Volatile
    private var installVerified = false

    /**
     * 使用 glob 模式搜索文件
     * @param glob glob 匹配模式
     * @param path 搜索路径，为 null 时搜索整个工作目录
     * @param cwd 工作目录
     * @param searchIgnore 是否搜索忽略文件（如 .gitignore）
     * @param ignoreCase 是否忽略大小写
     * @return 匹配文件路径的 Flow
     */
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

    /**
     * 使用正则模式搜索文件内容（grep 功能）
     * @param pattern 搜索正则模式
     * @param path 搜索路径，为 null 时搜索整个工作目录
     * @param cwd 工作目录
     * @param glob 文件名过滤模式
     * @param type 文件类型过滤
     * @param outputMode 输出模式："files_with_matches"、"count" 或默认的内容输出
     * @param before 显示匹配行之前的行数
     * @param after 显示匹配行之后的行数
     * @param context 显示匹配行前后的行数
     * @param lineNumbers 是否显示行号
     * @param caseInsensitive 是否忽略大小写
     * @param multiline 是否启用多行模式
     * @return 匹配结果的 Flow
     */
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

    /**
     * 从 JAR 资源中安装 ripgrep 二进制文件
     */
    @JvmStatic
    fun install() {
        CommandUtil.installFromJar(RG_RESOURCE_PATH, RG_BINARY_NAME, getJarvisBinDirectory())
    }

    /**
     * 确保 ripgrep 二进制文件已安装，支持多线程安全
     */
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

    /**
     * 解析 rg 二进制文件路径，确保已安装
     * @return rg 二进制文件的绝对路径
     */
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

    /**
     * 获取当前平台的 rg 二进制文件名（含扩展名）
     * @return 二进制文件名
     */
    private fun currentBinaryName(): String {
        return if (CommandUtil.getOSInfo().isWindows) "$RG_BINARY_NAME.exe" else RG_BINARY_NAME
    }

    /**
     * 确保文件具有可执行权限
     * @param file 目标文件
     */
    private fun ensureExecutable(file: File) {
        if (!file.canExecute()) {
            file.setExecutable(true, false)
        }
    }

    /**
     * 解析 glob 字符串，将空格和逗号分隔的 glob 表达式拆分为列表
     * @param glob glob 字符串（支持空格和逗号分隔的多个 glob 表达式）
     * @return 拆分后的 glob 表达式列表
     */
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
