package com.qifu.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

data class AgentConfig(
    val agentType: String,          // Agent identifier (matches subagent_type)
    val whenToUse: String,          // Description of when to use this agent
    val tools: Any,                 // Tool permissions: List<String> or "*"
    var systemPrompt: String,       // System prompt content
    val scope: Scope,         // 'built-in' | 'user' | 'project'
    val sourcePath: Path? = null,
    val originalContent: String? = null,       // Original content of the agent file
    val lastModifiedTime: String? = null,       // Last modified time of the agent file
) {
    enum class Scope(val value: String) {
        BUILT_IN("built-in"),
        USER("user"),
        PROJECT("project"),
        ;

        companion object {
            fun fromString(value: String): Scope? {
                return entries.find { it.value.equals(value, ignoreCase = true) }
            }
        }
    }
}

// 内置通用 Agent
private val BUILTIN_GENERAL_PURPOSE: AgentConfig = AgentConfig(
    agentType = "general-purpose",
    whenToUse = "General-purpose agent for researching complex questions, searching for code, and executing multi-step tasks. When you are searching for a keyword or file and are not confident that you will find the right match in the first few tries use this agent to perform the search for you.",
    tools = "*",
    systemPrompt = """
You are Jarvis, 奇富科技’s in-IDE AI assistant. Always respond in Chinese, unless the user explicitly requests another language.

Given the user's message, you should use the tools available to complete the task. Do what has been asked; nothing more, nothing less. When you complete the task simply respond with a detailed writeup.

Your strengths:
- Searching for code, configurations, and patterns across large codebases
- Analyzing multiple files to understand system architecture
- Investigating complex questions that require exploring many files
- Performing multi-step research tasks

Guidelines:
- For file searches: Use Grep or Glob when you need to search broadly. Use Read when you know the specific file path.
- For analysis: Start broad and narrow down. Use multiple search strategies if the first doesn't yield results.
- Be thorough: Check multiple locations, consider different naming conventions, look for related files.
- NEVER create files unless they're absolutely necessary for achieving your goal. ALWAYS prefer editing an existing file to creating a new one.
- NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested.
- In your final response always share relevant file names and code snippets. Any file paths you return in your response MUST be absolute. Do NOT use relative paths.
- For clear communication, avoid using emojis.


Notes:
- Agent threads always have their cwd reset between bash calls, as a result please only use absolute file paths.
- In your final response always share relevant file names and code snippets. Any file paths you return in your response MUST be absolute. Do NOT use relative paths.
- For clear communication with the user the assistant MUST avoid using emojis.

Here is useful information about the environment you are running in:
<env>
Working directory: {{CWD}}
Is directory a git repo: {{IS_GIT_REPO}}
Platform: {{PLATFORM}}
OS Version: {{OS_VERSION}}
Today's date: {{DATE}}
</env>
You are powered by the model {{MODEL_NAME}}
    """.trimIndent(),
    scope = AgentConfig.Scope.BUILT_IN,
)

// 内置 Explore Agent - 只读探索代理
private val BUILTIN_EXPLORE: AgentConfig = AgentConfig(
    agentType = "Explore",
    whenToUse = "File search specialist for thoroughly navigating and exploring codebases. Use for READ-ONLY tasks like finding files, searching code, and analyzing existing implementations. Best for: rapidly finding files using glob patterns, searching code with regex patterns, reading and analyzing file contents. Do NOT use for tasks that require creating or modifying files.",
    tools = listOf("Bash", "Glob", "Grep", "Read", "Skill", "TodoWrite", "ResolveClassName", "ReadClass", "ListImplementations"),
    systemPrompt = """
You are Jarvis, 奇富科技's in-IDE AI assistant.

You are a file search specialist. You excel at thoroughly navigating and exploring codebases.

=== CRITICAL: READ-ONLY MODE - NO FILE MODIFICATIONS ===
This is a READ-ONLY exploration task. You are STRICTLY PROHIBITED from:
- Creating new files (no Write, touch, or file creation of any kind)
- Modifying existing files (no Edit operations)
- Deleting files (no rm or deletion)
- Moving or copying files (no mv or cp)
- Creating temporary files anywhere, including /tmp
- Using redirect operators (>, >>, |) or heredocs to write to files
- Running ANY commands that change system state

Your role is EXCLUSIVELY to search and analyze existing code. You do NOT have access to file editing tools - attempting to edit files will fail.

Your strengths:
- Rapidly finding files using glob patterns
- Searching code and text with powerful regex patterns
- Reading and analyzing file contents

Guidelines:
- Use Glob for broad file pattern matching
- Use Grep for searching file contents with regex
- Use Read when you know the specific file path you need to read
- Use Bash ONLY for read-only operations (ls, git status, git log, git diff, find, cat, head, tail)
- NEVER use Bash for: mkdir, touch, rm, cp, mv, git add, git commit, npm install, pip install, or any file creation/modification
- Adapt your search approach based on the thoroughness level specified by the caller
- Return file paths as absolute paths in your final response
- For clear communication, avoid using emojis
- Communicate your final report directly as a regular message - do NOT attempt to create files

NOTE: You are meant to be a fast agent that returns output as quickly as possible. In order to achieve this you must:
- Make efficient use of the tools that you have at your disposal: be smart about how you search for files and implementations
- Wherever possible you should try to spawn multiple parallel tool calls for grepping and reading files

Complete the user's search request efficiently and report your findings clearly.

Notes:
- Agent threads always have their cwd reset between bash calls, as a result please only use absolute file paths.
- In your final response always share relevant file names and code snippets. Any file paths you return in your response MUST be absolute. Do NOT use relative paths.
- For clear communication with the user the assistant MUST avoid using emojis.

Here is useful information about the environment you are running in:
<env>
Working directory: {{CWD}}
Is directory a git repo: {{IS_GIT_REPO}}
Platform: {{PLATFORM}}
OS Version: {{OS_VERSION}}
Today's date: {{DATE}}
</env>
You are powered by the model {{MODEL_NAME}}
    """.trimIndent(),
    scope = AgentConfig.Scope.BUILT_IN,
)


class AgentLoader {

    private val LOG = Logger.getInstance(AgentLoader::class.java)
    private val yaml = Yaml()
    private val agentCache = ConcurrentHashMap<String, List<AgentConfig>>()
    private val watchers = mutableListOf<WatchService>()

    fun scanAgentDirectory(dirPath: Path, scope: AgentConfig.Scope, isRecordConfig: Boolean = false, project: Project? = null): List<AgentConfig> {
        val dir = dirPath.toFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val agents = mutableListOf<AgentConfig>()
        dir.listFiles { f -> f.extension == "md" }?.forEach { file ->
            try {
                val text = file.readText()
                val (front, body) = parseFrontmatter(text)
                val name = front["name"]?.toString()
                val desc = front["description"]?.toString()

                if (name.isNullOrBlank() || desc.isNullOrBlank()) {
                    LOG.warn("Skipping ${file.name}: missing required fields (name, description)")
                    return@forEach
                }

                val tools = parseTools(front["tools"])
                val subAgent = AgentConfig(
                    agentType = name,
                    whenToUse = desc.replace("\\n", "\n"),
                    tools = tools,
                    systemPrompt = body.trim(),
                    scope = scope,
                    sourcePath = file.toPath(),
                    lastModifiedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(file.lastModified()),
                    originalContent = text,
                )
                agents.add(subAgent)

                if (isRecordConfig) {
                    var isRecord = true
                    val path = when (scope) {
                        AgentConfig.Scope.PROJECT -> {
                            val projectRoot = GitUtil.getGitRoot(project!!)
                            if (projectRoot != null) {
                                file.relativeTo(File(projectRoot)).path
                            } else {
                                // todo 非git仓库配置也需处理
                                isRecord = false
                                file.toPath().toString()
                            }
                        }
                        else -> file.toPath().toString()
                    }
                    if (isRecord && !DumbService.isDumb(project!!)) {
                        // 索引构建完成后，git才有信息，这时保存
                        AgentConfigTraceUtil.traceAgentConfig(
                            subAgent,
                            toPosixPath(path),
                            project
                        )
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Failed to parse agent file ${file.name}: ${e.message}")
            }
        }
        return agents
    }

    private fun parseFrontmatter(content: String): Pair<Map<String, Any>, String> {
        try {
            val parts = content.replace("\r", "").split("---")
            if (parts.size < 3) return emptyMap<String, Any>() to content
            val yamlContent = parts[1]
            var body = parts.drop(2).joinToString("---")
//            McpPromptIntegration.generateMcpPromptSection().takeIf { it.isNotBlank() }?.let { body += "\n\n$it" }

            val data = yaml.load<Map<String, Any>>(yamlContent) ?: emptyMap()
            return data to body
        } catch (e: Exception)  {
            LOG.warn("Failed to parse agent frontmatter: ${e.message}")
            return emptyMap<String, Any>() to ""
        }
    }

    private fun parseTools(value: Any?): Any {
        return when (value) {
            is String -> if (value == "*") "*" else listOf(value)
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList<String>()
        }
    }

    data class AllAgents(val activeAgents: List<AgentConfig>, val allAgents: List<AgentConfig>)
    fun loadAllAgents(isRecordConfig: Boolean = false, project: Project? = null): AllAgents {
        return try {
            val userConfigDir = getUserConfigDirectory()
            val projectConfigDir = getProjectConfigDirectory()

            val userAgentDir = Paths.get(userConfigDir, "agents")
            val projectAgentDir = Paths.get(projectConfigDir, "agents")

            val userAgents = scanAgentDirectory(userAgentDir, AgentConfig.Scope.USER, isRecordConfig, project)
            val projectAgents = scanAgentDirectory(projectAgentDir, AgentConfig.Scope.PROJECT, isRecordConfig, project)

            val builtinAgents = listOf(BUILTIN_GENERAL_PURPOSE, BUILTIN_EXPLORE)

            val agentMap = linkedMapOf<String, AgentConfig>()
            (builtinAgents + userAgents + projectAgents).forEach { agentMap[it.agentType] = it }
            val activeAgents = agentMap.values.toList()
            val allAgents = builtinAgents + userAgents + projectAgents

            AllAgents(activeAgents, allAgents)
        } catch (e: Exception) {
            LOG.warn("Failed to load agents: ${e.message}")
            val fallback = listOf(BUILTIN_GENERAL_PURPOSE, BUILTIN_EXPLORE)
            AllAgents(fallback, fallback)
        }
    }

    fun getActiveAgents(): List<AgentConfig> =
        agentCache.getOrPut("active") {
            loadAllAgents().activeAgents
        }

    suspend fun getAllAgents(): List<AgentConfig> =
        agentCache.getOrPut("all") {
            loadAllAgents().allAgents
        }

    fun clearCache() {
        agentCache.clear()
    }

    fun startWatcher(onChange: (() -> Unit)? = null) {
        stopWatcher()

        val dirs = listOf(
            Paths.get(getUserConfigDirectory(), "agents"),
            Paths.get(getProjectConfigDirectory(), "agents"),
        )

        dirs.forEach { dir ->
            dir.toFile().mkdirs()
            val watchService = FileSystems.getDefault().newWatchService()
            dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            watchers += watchService
            Thread( {
                try {
                    while (true) {
                        val key = watchService.take()
                        for (event in key.pollEvents()) {
                            val kind = event.kind()
                            val filename = event.context().toString()
                            if (filename.endsWith(".md")) {
                                LOG.info("🔄 Agent configuration changed: $filename ($kind)")
                                clearCache()
                                onChange?.invoke()
                            }
                        }
                        key.reset()
                    }
                } catch (_: ClosedWatchServiceException) {}
            }, "AGENT_WATCHER").start()
        }
    }

    fun stopWatcher() {
        watchers.forEach {
            try {
                it.close()
            } catch (_: Exception) {
            }
        }
        watchers.clear()
    }
}