package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.PlatformUtils
import com.miracle.agent.mcp.McpClientHub
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import dev.langchain4j.agent.tool.ToolSpecification
import kotlin.reflect.full.companionObjectInstance

object ToolRegistry {

    private val LOG = Logger.getInstance(ToolRegistry::class.java)
    private val TOOLS = mutableMapOf<String, Tool<*>>()
    private val ASK_TOOL_NAMES = setOf(
        "Task",
        "Glob",
        "Grep",
        "ResolveClassName",
        "ReadClass",
        "Read",
        "ExcelRead",
//        LsTool.getName(),
//        McpTool.getName(),
        "AskUserQuestion",
        "Skill",
    )
    private val PLAN_TOOL_NAMES = setOf(
        "Glob",
        "Grep",
        "Read",
        "ResolveClassName",
        "ReadClass",
        "ListImplementations",
        "ExcelRead",
        "RequestUserInput",
    )

    private fun autoRegisterTools() {
        if (TOOLS.isNotEmpty()) return

        try {
            val toolClasses = listOf(
                TaskTool::class,
                BashTool::class,
                GlobTool::class,
                GrepTool::class,
                ReadTool::class,
                ExcelReadTool::class,
                EditTool::class,
                WriteTool::class,
//                LsTool::class,
//                MultiEditTool::class,
                TodoWriteTool::class,
                AskUserQuestionTool::class,
                RequestUserInputTool::class,
                SkillTool::class,
                // 这里可以添加更多实现Tool接口的类
//                McpTool::class,
            )
            
            toolClasses.forEach { toolClass ->
                try {
                    // 获取伴生对象实例
                    val companion = toolClass.companionObjectInstance
                    if (companion != null) {
                        // 如果有伴生对象，尝试获取实例
                        register(companion as Tool<*>)
                    } else {
                        // 尝试获取单例对象
                        val instance = toolClass.objectInstance
                        if (instance is Tool<*>) {
                            register(instance)
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to register tool from class ${toolClass.simpleName}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to auto-register tools", e)
        }
    }

    fun register(tool: Tool<*>) {
        try {
            val name = tool.getName()
            TOOLS[name] = tool
            LOG.info("Registered tool: $name")
        } catch (e: Exception) {
            LOG.warn("Failed to register tool instance", e)
        }
    }

    fun get(name: String): Tool<*>? {
        autoRegisterTools()
        return TOOLS[name] ?: getMcpTools()[name]
    }

    fun getToolSpecifications(): List<ToolSpecification> {
        autoRegisterTools()
        return getToolSpecifications(TOOLS.values.toList() + getMcpTools().values) ?: emptyList()
    }

    fun getToolSpecifications(tools: List<Tool<*>>?): List<ToolSpecification>? {
        return tools?.mapNotNull { tool ->
            try {
                tool.getToolSpecification()
            } catch (e: Exception) {
                LOG.warn("Failed to load tool specification for ${tool::class.simpleName}: ${e.message}")
                null
            }
        }
    }

    /**
     * 获取 Agent 模式下的工具集合
     */
    fun getAll(): Map<String, Tool<*>> {
        autoRegisterTools()
        return TOOLS + getMcpTools() + getDynamicTools()
    }

    fun getMcpTools(): Map<String, Tool<*>> {
        val instantiate = McpClientHub.instantiate()
        return instantiate.associateBy { it.getName() }
    }

    fun getDynamicTools(): Map<String, Tool<*>> {
        val dynamicTools = mutableMapOf<String, Tool<*>>()
        // 只在 Java IDE (IntelliJ IDEA) 中添加 ReadClassTool
        if (PlatformUtils.isIntelliJ()) {
            dynamicTools[ReadClassTool.getName()] = ReadClassTool
            dynamicTools[ResolveClassNameTool.getName()] = ResolveClassNameTool
            dynamicTools[ListImplementationsTool.getName()] = ListImplementationsTool
        }
        return dynamicTools
    }

    /**
     * 获取 ASK 模式下的工具集合
     */
    fun getAskTools(): Map<String, Tool<*>> {
        return getAll().filterKeys { it in ASK_TOOL_NAMES }
    }

    fun getPlanTools(): Map<String, Tool<*>> {
        return getAll().filterKeys { it in PLAN_TOOL_NAMES }
    }

    fun filterToolsForMode(chatMode: ChatMode, tools: List<Tool<*>>): List<Tool<*>> {
        val allowedNames = when (chatMode) {
            ChatMode.PLAN -> PLAN_TOOL_NAMES
            ChatMode.ASK -> ASK_TOOL_NAMES
            else -> null
        }
        return if (allowedNames == null) {
            tools
        } else {
            tools.filter { it.getName() in allowedNames }
        }
    }

    fun isPlanTool(name: String): Boolean {
        return PLAN_TOOL_NAMES.contains(name)
    }
}
