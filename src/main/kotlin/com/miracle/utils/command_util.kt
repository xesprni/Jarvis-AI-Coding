package com.miracle.utils

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption


data class OSInfo(
    val name: String,
    val path: String,
    val isWindows: Boolean
)

object CommandUtil {
    
    val LOG = thisLogger()

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
