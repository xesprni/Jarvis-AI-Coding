package com.qifu.agent.mcp

import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * MCP Prompt集成
 */
object McpPromptIntegration {

    private val prettyJson = Json {
        prettyPrint = true
    }

    //已注册的工具名称集合,不按照项目区分，只能自动审批使用!!
    val toolsNameSet: MutableSet<String> = mutableSetOf()

    //已注册的工具自动审批集合,不按照项目区分，只能自动审批使用!!
    val toolsAutoApproveSet: MutableSet<String> = mutableSetOf()


    /**
     * 生成完整的MCP服务器prompt描述
     */
    fun generateMcpPromptSection(project: Project): String {
        // 获取项目的MCP客户端管理器
        val hub = McpClientHub.getInstance(project)

        // 确保mcp服务已启动
        hub.ensureInitialized()
        // 从配置中读取已启用的MCP服务器
        val enabledServers = McpConfigManager.getEnabledServers(project)
        if (enabledServers.isEmpty()) {
            return ""
        }
        val allClients = hub.getAllClients()
        val specs = buildString {
            appendLine("# MCP Server Instructions")
            appendLine()
            appendLine("The following MCP servers have provided instructions for how to use their tools and resources:")
            appendLine()
            enabledServers.forEach { (serverName, _) ->
                //添加server prompt
                allClients[serverName]?.let { client ->
                    appendLine("## $serverName")
                    client.getClient().serverInstructions?.let {
                        appendLine(it)
                    }
                    appendLine()
                }
            }
        }

        return specs.trimEnd()
    }

}
