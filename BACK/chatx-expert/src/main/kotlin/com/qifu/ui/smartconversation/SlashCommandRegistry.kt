package com.qifu.ui.smartconversation

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.qifu.services.AgentService
import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.Icon

enum class SlashCommandCategory(val displayName: String, val order: Int) {
    BUILT_IN("Built-in", 0),
    SKILL("Skill", 1),
}

enum class SlashCommandScope {
    ALL,
    SKILLS_ONLY,
}

data class SlashCommand(
    val command: String,
    val description: String,
    val category: SlashCommandCategory,
    val argumentTemplates: List<String> = emptyList(),
    val icon: Icon? = null,
)

object SlashCommandRegistry {
    private val builtInCommands = listOf(
        SlashCommand(
            command = "/clear",
            description = "Reset conversation context and UI",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.Actions.Refresh,
        ),
        SlashCommand(
            command = "/compact",
            description = "Summarize and compact the current conversation",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.Actions.Collapseall,
        ),
    )

    @JvmStatic
    fun getCommands(): List<SlashCommand> = builtInCommands

    @JvmStatic
    fun getCommands(project: Project, scope: SlashCommandScope = SlashCommandScope.ALL): List<SlashCommand> {
        val commands = when (scope) {
            SlashCommandScope.ALL -> builtInCommands + getSkillCommands(project)
            SlashCommandScope.SKILLS_ONLY -> getSkillCommands(project)
        }
        return commands.sortedWith(
            compareBy<SlashCommand>({ it.category.order }, { it.command.lowercase() })
        )
    }

    @JvmStatic
    fun findCommand(input: String): SlashCommand? {
        val trimmed = input.trim()
        return builtInCommands.firstOrNull { it.command.equals(trimmed, ignoreCase = true) }
    }

    private fun getSkillCommands(project: Project): List<SlashCommand> {
        val occupiedCommands = builtInCommands.map { it.command.lowercase() }.toSet()
        val skillLoader = project.service<AgentService>().skillLoader
        return skillLoader.getActiveSkills()
            .asSequence()
            .mapNotNull { skill ->
                val command = toSlashCommand(skill.name)
                if (command.isBlank()) {
                    null
                } else {
                    SlashCommand(
                        command = command,
                        description = summarizeDescription(skill.description),
                        category = SlashCommandCategory.SKILL,
                        argumentTemplates = skill.argumentHints,
                        icon = Icons.SKILLS,
                    )
                }
            }
            .filterNot { it.command.lowercase() in occupiedCommands }
            .distinctBy { it.command.lowercase() }
            .toList()
    }

    private fun toSlashCommand(name: String): String {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return ""
        }
        return if (trimmedName.startsWith("/")) {
            trimmedName
        } else {
            "/$trimmedName"
        }
    }

    private fun summarizeDescription(description: String): String {
        val firstLine = description
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
        return firstLine.ifBlank {
            "Run skill"
        }
    }
}
