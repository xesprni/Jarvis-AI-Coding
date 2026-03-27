package com.miracle.ui.core.composer

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.miracle.services.AgentService
import com.miracle.utils.SkillConfig
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
            description = "Reset the current conversation context and UI",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.Actions.Refresh,
        ),
        SlashCommand(
            command = "/new",
            description = "Archive the current conversation and open a new chat tab",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.General.Add,
        ),
        SlashCommand(
            command = "/compact",
            description = "Summarize and compact the current conversation context",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.Actions.Collapseall,
        ),
        SlashCommand(
            command = "/plan",
            description = "Switch to Plan mode for read-only planning",
            category = SlashCommandCategory.BUILT_IN,
            icon = AllIcons.Actions.MenuOpen,
        ),
    )

    @JvmStatic
    fun findBuiltInCommand(input: String): SlashCommand? {
        val trimmed = input.trim()
        return builtInCommands.firstOrNull { it.command.equals(trimmed, ignoreCase = true) }
    }

    @JvmStatic
    fun getCommands(project: Project, scope: SlashCommandScope = SlashCommandScope.ALL): List<SlashCommand> {
        val activeSkills = project.service<AgentService>().skillLoader.getActiveSkills()
        return getCommands(activeSkills, scope)
    }

    @JvmStatic
    fun getCommands(skills: List<SkillConfig>, scope: SlashCommandScope = SlashCommandScope.ALL): List<SlashCommand> {
        val skillCommands = getSkillCommands(skills)
        return when (scope) {
            SlashCommandScope.ALL -> (builtInCommands + skillCommands)
            SlashCommandScope.SKILLS_ONLY -> skillCommands
        }.sortedWith(compareBy<SlashCommand>({ it.category.order }, { it.command.lowercase() }))
    }

    private fun getSkillCommands(skills: List<SkillConfig>): List<SlashCommand> {
        val occupiedCommands = builtInCommands.map { it.command.lowercase() }.toSet()
        return skills.asSequence()
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
                        icon = AllIcons.Nodes.Plugin,
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
        return if (trimmedName.startsWith("/")) trimmedName else "/$trimmedName"
    }

    private fun summarizeDescription(description: String): String {
        val firstLine = description.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
        return firstLine.ifBlank { "Run skill" }
    }
}
