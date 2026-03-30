package com.miracle.utils

import com.miracle.config.AutoApproveSettings

/**
 * 命令安全检查工具，用于判断命令是否允许执行
 */
object SecurityUtil {

    /** 被禁止执行的命令集合 */
    val BANNED_COMMANDS = setOf(
        "rmdir", "format", "fdisk", "mkfs",
        "shutdown", "reboot", "halt", "poweroff",
        "sudo", "su", "passwd", "chpasswd",
        "chmod", "chown", "chgrp",
        "dd", "shred", "wipe"
    )
    
    /**
     * 提取命令中所有子命令的首个单词
     * @param command 完整命令字符串（可能包含 &&、||、|、; 分隔的多个子命令）
     * @return 子命令首个单词的集合
     */
    fun getCommandFirstWords(command: String): Set<String> {
        val trimmedCommand = command.trim()
        val commands = trimmedCommand.split(Regex("\\s*(&&|\\|\\||\\||;)\\s*"))
        return commands.filter { it.isNotEmpty() }
            .map { it.split(Regex("\\s+"))[0].lowercase() }
            .toSet()
    }

    /**
     * 判断命令是否属于禁止执行的命令
     * @param command 完整命令字符串
     * @return 是否为禁止命令
     */
    fun isBannedCommand(command: String): Boolean {
        val firstWords = getCommandFirstWords(command)
        return firstWords.any { it in BANNED_COMMANDS }
    }
    
    /**
     * 判断命令是否为只读命令（不修改文件系统）
     * @param command 完整命令字符串
     * @return 是否为只读命令
     */
    fun isReadOnlyCommand(command: String): Boolean {
        val readOnlyCommands = setOf(
            "ls", "ll", "netstat", "telnet", "tasklist", "cat", "grep", "find", "head", "tail", "stat", "pwd",
            "whoami", "which", "ps", "top", "df", "du", "lsof", "ping", "curl", "sed", "less", "more", "wc", "sort",
            "dir", "pytest", "uv run", "npm run",
        )
        val firstWords = getCommandFirstWords(command)
        return firstWords.all { it in readOnlyCommands || it.startsWith("#") || it.isEmpty() }
    }

    /**
     * 判断命令是否在用户自定义的黑名单中
     * @param command 完整命令字符串
     * @return 是否为黑名单命令
     */
    fun isBlacklistedCommand(command: String): Boolean {
        val trimmedCommand = command.trim()
        var commands = trimmedCommand.split(Regex("\\s*(&&|\\|\\||\\||;)\\s*"))
        commands = commands.filter { it.isNotEmpty() }.toList()
        return commands.any {
            for (blacklistedCommand in AutoApproveSettings.state.autoRunCommandsBlacklist) {
                if (it.startsWith(blacklistedCommand)) {
                    return true
                }
            }
            return false
        }
    }
    
}