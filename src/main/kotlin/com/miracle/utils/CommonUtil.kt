package com.miracle.utils

import java.io.File

object CommonUtil {

    /**
     * 在当前系统上检查某个命令是否可用（跨平台）。
     * - 如果是绝对/相对路径，则判断文件是否存在且可执行；
     * - 否则遍历 PATH 以及常见补充目录（macOS GUI 进程下 PATH 可能不完整）。
     */
    @JvmStatic
    fun isCommandAvailable(command: String): Boolean {
        if (command.isBlank()) return false
        // 路径形式（包含路径分隔符）
        if (command.contains('/') || command.contains('\\')) {
            val f = File(command)
            return f.exists() && f.canExecute()
        }
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val path = System.getenv("PATH").orEmpty()
        val searchDirs = path.split(File.pathSeparator).filter { it.isNotBlank() }.toMutableSet()
        // 补充常见目录
        val extraDirs = listOf(
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/usr/bin",
            "/bin",
            "/usr/sbin",
            "/sbin"
        )
        searchDirs.addAll(extraDirs)
        val pathext = if (isWindows) {
            (System.getenv("PATHEXT") ?: ".EXE;.BAT;.CMD;.COM;.PS1")
                .split(";")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
        } else emptyList()
        val candidateNames: List<String> = if (isWindows) {
            val lower = command.lowercase()
            (pathext.map { lower + it } + command).distinct()
        } else listOf(command)
        for (dir in searchDirs) {
            val base = File(dir)
            if (!base.exists()) continue
            for (name in candidateNames) {
                val f = File(base, name)
                if (f.exists() && (if (isWindows) true else f.canExecute())) {
                    return true
                }
            }
        }
        return false
    }
}