package com.miracle.services

import com.intellij.openapi.diagnostic.Logger
import com.miracle.utils.getCurrentProjectRootPath
import java.nio.file.Files
import java.nio.file.Paths

private val LOG = Logger.getInstance("com.miracle.services.context")

/**
 * 获取当前项目的文档
 */
fun getProjectInstruction() : String? {
    return try {
        val cwd = getCurrentProjectRootPath()
        val agentsPath = Paths.get(cwd, "AGENTS.md")
        val jarvisPath = Paths.get(cwd, "JARVIS.md")

        val docs = mutableListOf<String>()
        if (Files.exists(agentsPath)) {
            try {
                val content = Files.readString(agentsPath)
                docs.add("# Contents of ${agentsPath.toAbsolutePath()} (project instructions, checked into the codebase):\n\n$content")
            } catch (e: Exception) {
                LOG.warn("Failed to read AGENTS.md", e)
            }
        }

        if (Files.exists(jarvisPath)) {
            try {
                val content = Files.readString(jarvisPath)
                docs.add("# Contents of ${jarvisPath.toAbsolutePath()} (project instructions, checked into the codebase):\n\n$content")
            } catch (e: Exception) {
                LOG.warn("Failed to read CLAUDE.md", e)
            }
        }

        if (docs.isNotEmpty()) docs.joinToString("\n\n---\n\n") else null
    } catch (e: Exception) {
        LOG.warn(e)
        null
    }
}