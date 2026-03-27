package com.qifu.utils

import com.qifu.config.AutoApproveSettings

object SecurityUtil {

    val BANNED_COMMANDS = setOf(
        "rmdir", "format", "fdisk", "mkfs",
        "shutdown", "reboot", "halt", "poweroff",
        "sudo", "su", "passwd", "chpasswd",
        "chmod", "chown", "chgrp",
        "dd", "shred", "wipe"
    )
    
    fun getCommandFirstWords(command: String): Set<String> {
        val trimmedCommand = command.trim()
        val commands = trimmedCommand.split(Regex("\\s*(&&|\\|\\||\\||;)\\s*"))
        return commands.filter { it.isNotEmpty() }
            .map { it.split(Regex("\\s+"))[0].lowercase() }
            .toSet()
    }

    fun isBannedCommand(command: String): Boolean {
        val firstWords = getCommandFirstWords(command)
        return firstWords.any { it in BANNED_COMMANDS }
    }
    
    fun isReadOnlyCommand(command: String): Boolean {
        val readOnlyCommands = setOf(
            "ls", "ll", "netstat", "telnet", "tasklist", "cat", "grep", "find", "head", "tail", "stat", "pwd",
            "whoami", "which", "ps", "top", "df", "du", "lsof", "ping", "curl", "sed", "less", "more", "wc", "sort",
            "dir", "pytest", "uv run", "npm run",
        )
        val firstWords = getCommandFirstWords(command)
        return firstWords.all { it in readOnlyCommands || it.startsWith("#") || it.isEmpty() }
    }

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