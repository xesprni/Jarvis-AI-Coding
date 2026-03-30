package com.miracle.ui.core.composer

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.miracle.services.AgentService
import com.miracle.utils.SkillConfig
import javax.swing.Icon

/**
 * 斜杠命令的分类枚举，用于区分内置命令和技能命令。
 *
 * @property displayName 分类显示名称
 * @property order 排序权重，值越小越靠前
 */
enum class SlashCommandCategory(val displayName: String, val order: Int) {
    BUILT_IN("Built-in", 0),
    SKILL("Skill", 1),
}

/**
 * 斜杠命令查找范围枚举，用于控制返回哪些类型的命令。
 */
enum class SlashCommandScope {
    /** 返回所有命令，包括内置命令和技能命令 */
    ALL,
    /** 仅返回技能命令 */
    SKILLS_ONLY,
}

/**
 * 斜杠命令数据类，描述一个可用的斜杠命令及其元信息。
 *
 * @property command 命令字符串，如 "/clear"
 * @property description 命令描述信息
 * @property category 命令分类
 * @property argumentTemplates 命令参数模板列表
 * @property icon 命令图标
 */
data class SlashCommand(
    val command: String,
    val description: String,
    val category: SlashCommandCategory,
    val argumentTemplates: List<String> = emptyList(),
    val icon: Icon? = null,
)

/**
 * 斜杠命令注册中心，统一管理内置命令和从技能加载的命令，
 * 提供命令查找和列表获取功能。
 */
object SlashCommandRegistry {

    /** 内置命令列表 */
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

    /**
     * 根据输入文本查找匹配的内置命令。
     *
     * @param input 用户输入的文本
     * @return 匹配的内置命令，未找到时返回 null
     */
    @JvmStatic
    fun findBuiltInCommand(input: String): SlashCommand? {
        val trimmed = input.trim()
        return builtInCommands.firstOrNull { it.command.equals(trimmed, ignoreCase = true) }
    }

    /**
     * 获取指定范围内的命令列表，从项目中的技能加载器获取活跃技能。
     *
     * @param project 当前项目实例
     * @param scope 命令查找范围，默认为 ALL
     * @return 排序后的命令列表
     */
    @JvmStatic
    fun getCommands(project: Project, scope: SlashCommandScope = SlashCommandScope.ALL): List<SlashCommand> {
        val activeSkills = project.service<AgentService>().skillLoader.getActiveSkills()
        return getCommands(activeSkills, scope)
    }

    /**
     * 根据技能列表和范围获取命令列表。
     *
     * @param skills 技能配置列表
     * @param scope 命令查找范围，默认为 ALL
     * @return 按分类和命令名排序后的命令列表
     */
    @JvmStatic
    fun getCommands(skills: List<SkillConfig>, scope: SlashCommandScope = SlashCommandScope.ALL): List<SlashCommand> {
        val skillCommands = getSkillCommands(skills)
        return when (scope) {
            SlashCommandScope.ALL -> (builtInCommands + skillCommands)
            SlashCommandScope.SKILLS_ONLY -> skillCommands
        }.sortedWith(compareBy<SlashCommand>({ it.category.order }, { it.command.lowercase() }))
    }

    /**
     * 从技能配置列表中生成斜杠命令，过滤掉与内置命令冲突和重复的命令。
     *
     * @param skills 技能配置列表
     * @return 生成的技能命令列表
     */
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

    /**
     * 将技能名称转换为斜杠命令格式，确保以 "/" 开头。
     *
     * @param name 技能名称
     * @return 格式化后的斜杠命令字符串
     */
    private fun toSlashCommand(name: String): String {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return ""
        }
        return if (trimmedName.startsWith("/")) trimmedName else "/$trimmedName"
    }

    /**
     * 提取描述文本的第一行非空内容作为摘要。
     *
     * @param description 原始描述文本
     * @return 第一行非空文本，为空时返回默认描述
     */
    private fun summarizeDescription(description: String): String {
        val firstLine = description.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
        return firstLine.ifBlank { "Run skill" }
    }
}
