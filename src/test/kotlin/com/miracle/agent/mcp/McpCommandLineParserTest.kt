package com.miracle.agent.mcp

import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals

class McpCommandLineParserTest {

    @Test
    fun normalizeSplitsInlineCommandAndAppendsExplicitArgs() {
        val normalized = McpCommandLineParser.normalize(
            "npx -y @modelcontextprotocol/server-filesystem",
            listOf("/tmp")
        )

        assertEquals("npx", normalized.command)
        assertEquals(
            listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
            normalized.args
        )
    }

    @Test
    fun normalizeKeepsExecutablePathWithWhitespaceAsSingleCommand() {
        val tempDir = Files.createTempDirectory("mcp-command-parser")
        val executable = tempDir.resolve("fake tool").toFile().apply {
            writeText("#!/bin/sh\n")
            setExecutable(true)
        }

        try {
            val normalized = McpCommandLineParser.normalize(executable.absolutePath, listOf("--help"))

            assertEquals(executable.absolutePath, normalized.command)
            assertEquals(listOf("--help"), normalized.args)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun normalizeRespectsQuotedArguments() {
        val normalized = McpCommandLineParser.normalize(
            "node \"script with spaces.js\" '--flag value'"
        )

        assertEquals("node", normalized.command)
        assertEquals(listOf("script with spaces.js", "--flag value"), normalized.args)
    }
}
