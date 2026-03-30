package com.miracle.agent.tool

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.PlatformUtils
import com.miracle.agent.mcp.McpClientHub
import com.miracle.ui.smartconversation.settings.configuration.ChatMode
import dev.langchain4j.agent.tool.ToolSpecification
import kotlin.reflect.full.companionObjectInstance

/**
 * 工具注册中心，负责管理和注册所有可用的工具实例。
 * 支持按聊天模式（Agent、ASK、Plan）过滤工具集合，并整合 MCP 工具和动态工具。
 */
object ToolRegistry {

    private val LOG = Logger.getInstance(ToolRegistry::class.java)
    // 已注册的工具实例映射表，key 为工具名称
    private val TOOLS = mutableMapOf<String, Tool<*>>()
    // ASK 模式下允许使用的工具名称集合
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
    // Plan 模式下允许使用的工具名称集合
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

    /**
     * 自动注册所有内置工具实例，仅在首次调用时执行
     */
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

    /**
     * 注册一个工具实例到注册中心
     * @param tool 要注册的工具实例
     */
    fun register(tool: Tool<*>) {
        try {
            val name = tool.getName()
            TOOLS[name] = tool
            LOG.info("Registered tool: $name")
        } catch (e: Exception) {
            LOG.warn("Failed to register tool instance", e)
        }
    }

    /**
     * 根据名称获取工具实例
     * @param name 工具名称
     * @return 对应的工具实例，未找到时返回 null
     */
    fun get(name: String): Tool<*>? {
        autoRegisterTools()
        return TOOLS[name] ?: getMcpTools()[name]
    }

    /**
     * 获取所有已注册工具的工具规格列表
     * @return 工具规格列表
     */
    fun getToolSpecifications(): List<ToolSpecification> {
        autoRegisterTools()
        return getToolSpecifications(TOOLS.values.toList() + getMcpTools().values) ?: emptyList()
    }

    /**
     * 将工具列表转换为工具规格列表
     * @param tools 工具实例列表
     * @return 工具规格列表，转换失败的工具会被过滤掉
     */
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

    /**
     * 获取所有 MCP 工具
     * @return MCP 工具映射表
     */
    fun getMcpTools(): Map<String, Tool<*>> {
        val instantiate = McpClientHub.instantiate()
        return instantiate.associateBy { it.getName() }
    }

    /**
     * 获取动态工具集合（仅在特定 IDE 平台上可用的工具）
     * @return 动态工具映射表
     */
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

    /**
     * 获取 Plan 模式下的工具集合
     */
    fun getPlanTools(): Map<String, Tool<*>> {
        return getAll().filterKeys { it in PLAN_TOOL_NAMES }
    }

    /**
     * 根据聊天模式过滤工具列表
     * @param chatMode 当前聊天模式
     * @param tools 待过滤的工具列表
     * @return 过滤后的工具列表
     */
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

    /**
     * 判断指定工具是否为 Plan 模式允许的工具
     * @param name 工具名称
     * @return 是否为 Plan 模式工具
     */
    fun isPlanTool(name: String): Boolean {
        return PLAN_TOOL_NAMES.contains(name)
    }
}
