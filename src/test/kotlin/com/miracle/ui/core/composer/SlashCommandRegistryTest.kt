package com.miracle.ui.core.composer

import com.miracle.utils.SkillConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SlashCommandRegistryTest {

    @Test
    fun testGetCommandsMergesBuiltInsAndSkillsWithoutDuplicates() {
        val skills = listOf(
            SkillConfig(
                name = "review",
                description = "Review code changes",
                content = "",
                filePath = "/tmp/review/SKILL.md",
                baseDir = "/tmp/review",
                argumentHints = listOf("<target>"),
                scope = SkillConfig.Scope.PROJECT,
            ),
            SkillConfig(
                name = "clear",
                description = "Should not override built-in clear",
                content = "",
                filePath = "/tmp/clear/SKILL.md",
                baseDir = "/tmp/clear",
                scope = SkillConfig.Scope.PROJECT,
            ),
        )

        val commands = SlashCommandRegistry.getCommands(skills, SlashCommandScope.ALL)

        assertEquals(listOf("/clear", "/compact", "/new", "/plan", "/review"), commands.map { it.command })
        assertEquals(listOf("<target>"), commands.last().argumentTemplates)
        assertEquals(SlashCommandCategory.SKILL, commands.last().category)
    }

    @Test
    fun testSkillsOnlyScopeExcludesBuiltIns() {
        val skills = listOf(
            SkillConfig(
                name = "custom-skill",
                description = "Custom skill work",
                content = "",
                filePath = "/tmp/custom-skill/SKILL.md",
                baseDir = "/tmp/custom-skill",
                scope = SkillConfig.Scope.PROJECT,
            )
        )

        val commands = SlashCommandRegistry.getCommands(skills, SlashCommandScope.SKILLS_ONLY)

        assertEquals(listOf("/custom-skill"), commands.map { it.command })
        assertTrue(commands.all { it.category == SlashCommandCategory.SKILL })
    }

    @Test
    fun testFindBuiltInCommandIgnoresCase() {
        val command = SlashCommandRegistry.findBuiltInCommand(" /CoMpAcT ")
        assertNotNull(command)
        assertEquals("/compact", command.command)
    }

    @Test
    fun testFindBuiltInNewCommand() {
        val command = SlashCommandRegistry.findBuiltInCommand("/NEW")
        assertNotNull(command)
        assertEquals("/new", command.command)
    }

    @Test
    fun testFindBuiltInPlanCommand() {
        val command = SlashCommandRegistry.findBuiltInCommand(" /PLAN ")
        assertNotNull(command)
        assertEquals("/plan", command.command)
    }
}
