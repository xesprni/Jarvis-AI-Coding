package com.miracle.agent.mcp

import java.io.File

/**
 * stdio MCP 配置中的命令行标准化结果。
 */
internal data class McpCommandLine(
    val command: String,
    val args: List<String>,
)

/**
 * 兼容 MCP stdio 配置中的两种常见写法：
 * 1. command 单独填写可执行文件，args 单独填写参数
 * 2. 整条命令直接填写在 command 中
 */
internal object McpCommandLineParser {

    fun normalize(commandText: String, explicitArgs: List<String> = emptyList()): McpCommandLine {
        val trimmedCommand = commandText.trim()
        if (trimmedCommand.isBlank()) {
            return McpCommandLine("", explicitArgs)
        }

        if (shouldTreatAsSingleExecutable(trimmedCommand)) {
            return McpCommandLine(trimMatchingQuotes(trimmedCommand), explicitArgs)
        }

        val tokens = tokenize(trimmedCommand)
        if (tokens.isEmpty()) {
            return McpCommandLine(trimmedCommand, explicitArgs)
        }

        return McpCommandLine(
            command = tokens.first(),
            args = tokens.drop(1) + explicitArgs,
        )
    }

    fun tokenize(commandText: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quotedBy: Char? = null
        var escaping = false

        commandText.forEach { ch ->
            when {
                escaping -> {
                    current.append(ch)
                    escaping = false
                }

                ch == '\\' -> escaping = true

                quotedBy != null && ch == quotedBy -> quotedBy = null

                quotedBy == null && (ch == '"' || ch == '\'') -> quotedBy = ch

                quotedBy == null && ch.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }

                else -> current.append(ch)
            }
        }

        if (escaping) {
            current.append('\\')
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    private fun shouldTreatAsSingleExecutable(commandText: String): Boolean {
        if (!commandText.any(Char::isWhitespace)) {
            return true
        }
        val executablePath = trimMatchingQuotes(commandText)
        return File(executablePath).exists()
    }

    private fun trimMatchingQuotes(text: String): String {
        if (text.length < 2) {
            return text
        }
        val first = text.first()
        val last = text.last()
        return if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            text.substring(1, text.length - 1)
        } else {
            text
        }
    }
}
