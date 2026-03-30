package com.miracle.utils

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption


/**
 * 操作系统信息数据类
 */
data class OSInfo(
    /** 操作系统名称 */
    val name: String,
    /** 平台架构路径标识（如 "mac_aarch64"） */
    val path: String,
    /** 是否为 Windows 系统 */
    val isWindows: Boolean
)

/**
 * 命令行执行工具类，提供进程运行、OS 检测和二进制文件安装功能
 */
object CommandUtil {
    
    /** 日志记录器 */
    val LOG = thisLogger()

    /**
     * 运行命令行并返回输出流
     * @param command 命令及参数列表
     * @param cwd 工作目录，默认为当前用户目录
     * @param timeoutMillis 超时时间（毫秒），默认 120 秒
     * @return 命令输出的 Flow，每行作为一个元素
     * @throws RuntimeException 超时或执行失败时抛出
     */
    fun run(
        command: List<String>,
        cwd: String? = System.getProperty("user.dir"),
        timeoutMillis: Long? = 120_000,
    ): Flow<String> = channelFlow {
        val process = ProcessBuilder(command)
            .apply {
                cwd?.let { directory(File(it)) }
                redirectErrorStream(true)
            }
            .start()
        process.outputStream.close()

        val reader = process.inputStream.bufferedReader()
        val readerJob = CoroutineScope(Dispatchers.IO).launch {
            for (line in reader.lineSequence()) {
                send(line)
            }
        }

        val finished = try {
            if (timeoutMillis != null && timeoutMillis > 0) {
                withTimeout(timeoutMillis) {
                    process.awaitExit()
                }
            } else {
                process.awaitExit()
            }
            true
        } catch (e: TimeoutCancellationException) {
            false
        }

        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("[TIMEOUT] Process killed after ${timeoutMillis}ms")
        } else {
            readerJob.join()
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                LOG.info("[EXIT] Process completed with exit code: $exitCode")
            }
        }
    }

    /**
     * 获取当前操作系统信息，包括名称、架构路径和是否为 Windows
     * @return 操作系统信息
     */
    fun getOSInfo(): OSInfo {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        LOG.info("-------------------> OS: $osName, Arch: $osArch")

        return when {
            osName.contains("win") -> {
                val archPath = when {
                    osArch.contains("aarch64") || osArch.contains("arm64") -> "win_aarch64"
                    else -> "win_x86_64"
                }
                OSInfo(name = "Windows", path = archPath, isWindows = true)
            }
            osName.contains("mac") -> {
                val archPath = when {
                    osArch.contains("aarch64") || osArch.contains("arm64") -> "mac_aarch64"
                    else -> "mac_x86_64"
                }
                OSInfo(name = "macOS", path = archPath, isWindows = false)
            }
            else -> {
                val archPath = when {
                    osArch.contains("aarch64") || osArch.contains("arm64") -> "linux_aarch64"
                    else -> "linux_x86_64"
                }
                OSInfo(name = "Linux", path = archPath, isWindows = false)
            }
        }
    }

    /**
     * 从JAR资源文件中安装二进制可执行文件
     *
     * @param baseDir 资源文件的基础目录路径（例如："/binaries"）
     * @param binaryBaseName 二进制文件的基础名称（不包含扩展名，例如："mytool"）
     * @param destinationDir 目标安装目录路径
     * @throws RuntimeException 当资源文件未找到或安装失败时抛出
     * 
     * 示例：
     * ```
     * installFromJar("/binaries", "rg", "/usr/local/bin")
     * ```
     * 
     * 在Windows系统上会安装为"rg.exe"，在其他系统上安装为"rg"
     */
    @JvmStatic
    fun installFromJar(baseDir: String, binaryBaseName: String, destinationDir: String) {
        try {
            val osInfo = getOSInfo()
            val binaryName = if (osInfo.isWindows) "${binaryBaseName}.exe" else binaryBaseName
            val resourcePath = baseDir.trimEnd('/') + '/' + osInfo.path + '/' + binaryName
            val targetFile = File(destinationDir, binaryName)
            if (targetFile.exists()) {
                if (!targetFile.canExecute()) {
                    targetFile.setExecutable(true, false)
                }
                LOG.info("$binaryBaseName already installed at: ${targetFile.absolutePath}")
                return
            }

            // 从资源文件复制
            val resourceStream = this::class.java.getResourceAsStream(resourcePath)
            if (resourceStream == null) {
                LOG.warn("$binaryBaseName binary not found in resources: $resourcePath")
                throw RuntimeException("$binaryBaseName binary not found for ${osInfo.name}")
            }
            Files.copy(resourceStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            resourceStream.close()

            // 设置可执行权限（非Windows系统）
            if (!osInfo.isWindows) {
                targetFile.setExecutable(true, false)
            }
            LOG.info("$binaryBaseName installed successfully at: ${targetFile.absolutePath}")
        } catch (e: Exception) {
            LOG.warn("Failed to install $binaryBaseName", e)
            throw RuntimeException("Failed to install $binaryBaseName: ${e.message}", e)
        }
    }
}
