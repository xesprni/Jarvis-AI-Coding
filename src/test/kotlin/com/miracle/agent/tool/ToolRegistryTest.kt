package com.miracle.agent.tool

import kotlin.test.*

class ToolRegistryTest {

    @Test
    fun testAutoRegistration() {
        // 测试ReadTool是否被自动注册
        val readTool = ToolRegistry.get("Read")
        assertNotNull(readTool, "Read tool should be registered automatically")
        
        println("Read tool registered successfully")
    }

    @Test
    fun testGetNonExistentTool() {
        // 测试获取不存在的工具
        val nonExistentTool = ToolRegistry.get("NonExistent")
        assertNull(nonExistentTool, "Non-existent tool should return null")
    }

    @Test
    fun testGetAllTools() {
        // 测试获取所有工具
        val allTools = ToolRegistry.getAll()
        assertNotNull(allTools, "getAll() should return a non-null map")
        assertTrue(allTools.isNotEmpty(), "Should have at least one tool registered")
        assertTrue(allTools.containsKey("Read"), "Should contain Read tool")
    }

    @Test
    fun testToolSpecificationRetrieval() {
        // 测试工具规格获取
        val specifications = ToolRegistry.getToolSpecifications()
        assertNotNull(specifications, "getToolSpecifications() should return a non-null list")
        assertTrue(specifications.isNotEmpty(), "Should have at least one tool specification")
        
        // 验证Read工具的规格
        val readSpec = specifications.find { it.name() == "Read" }
        assertNotNull(readSpec, "Should find Read tool specification")
        assertEquals("Read", readSpec.name())
        assertNotNull(readSpec.description(), "Read tool should have a description")
    }

    @Test
    fun testGetPlanTools() {
        // 测试获取 Plan 模式下的工具集合
        val planTools = ToolRegistry.getPlanTools()
        assertNotNull(planTools, "getPlanTools() should return a non-null map")
        assertTrue(planTools.isNotEmpty(), "Should have at least one plan tool")
        
        // 验证 Plan 模式下允许的工具
        assertTrue(planTools.containsKey("Read"), "Plan mode should include Read tool")
        assertTrue(planTools.containsKey("Grep"), "Plan mode should include Grep tool")
        assertTrue(planTools.containsKey("Glob"), "Plan mode should include Glob tool")
        assertTrue(planTools.containsKey("Write"), "Plan mode should include Write tool")
        assertTrue(planTools.containsKey("ExitPlanMode"), "Plan mode should include ExitPlanMode tool")
        
        // 验证 Plan 模式下不应该包含的工具
        assertFalse(planTools.containsKey("Bash"), "Plan mode should NOT include Bash tool")
        assertFalse(planTools.containsKey("Edit"), "Plan mode should NOT include Edit tool")
        
        println("Plan mode tools: ${planTools.keys}")
    }

    @Test
    fun testGetAskTools() {
        // 测试获取 ASK 模式下的工具集合
        val askTools = ToolRegistry.getAskTools()
        assertNotNull(askTools, "getAskTools() should return a non-null map")
        assertTrue(askTools.isNotEmpty(), "Should have at least one ask tool")
        
        // 验证 ASK 模式下允许的工具
        assertTrue(askTools.containsKey("Read"), "Ask mode should include Read tool")
        assertTrue(askTools.containsKey("Grep"), "Ask mode should include Grep tool")
        assertTrue(askTools.containsKey("AskUserQuestion"), "Ask mode should include AskUserQuestion tool")
        
        println("Ask mode tools: ${askTools.keys}")
    }

    @Test
    fun testPlanToolSpecifications() {
        // 测试 Plan 模式下的工具规范
        val planTools = ToolRegistry.getPlanTools().values.toList()
        val planSpecs = ToolRegistry.getToolSpecifications(planTools)
        
        assertNotNull(planSpecs, "Plan tool specifications should not be null")
        assertTrue(planSpecs!!.isNotEmpty(), "Should have at least one plan tool specification")
        
        // 验证只包含 Plan 模式的工具
        val planToolNames = planSpecs.map { it.name() }
        assertTrue(planToolNames.contains("Read"), "Plan specs should include Read")
        assertFalse(planToolNames.contains("Bash"), "Plan specs should NOT include Bash")
        
        println("Plan tool specifications: $planToolNames")
    }
}
