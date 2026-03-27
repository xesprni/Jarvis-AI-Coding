package com.qifu.agent.tool

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.qifu.agent.AgentMessageType
import com.qifu.agent.JarvisSay
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.utils.JsonField
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KFunction

/**
 * 解析后的类信息
 */
data class ResolvedClassInfo(
    val fqn: String,
    val type: String,
    val source: String
)

/**
 * 工具输出数据
 */
data class ResolveClassNameOutput(
    val results: List<ResolvedClassInfo>,
    val truncated: Boolean,
    val durationMs: Long,
)

/**
 * 将 PsiClass 转换为 ResolvedClassInfo
 */
fun PsiClass.toResolvedInfo(): ResolvedClassInfo {
    val fqn = this.qualifiedName ?: return ResolvedClassInfo(
        fqn = this.name ?: "<anonymous>",
        type = "class",
        source = "unknown"
    )

    val type = when {
        this.isInterface -> "interface"
        this.isEnum -> "enum"
        this.isAnnotationType -> "annotation"
        else -> "class"
    }

    val source = when {
        this.containingFile?.virtualFile?.fileSystem?.protocol == "jar" -> "dependency"
        else -> "project"
    }

    return ResolvedClassInfo(
        fqn = fqn,
        type = type,
        source = source
    )
}

/**
 * 解析类名为完全限定名（支持简单名和FQN）
 */
fun resolveClassName(project: Project, name: String): List<ResolvedClassInfo> {
    val scope = GlobalSearchScope.allScope(project)
    val result = mutableListOf<ResolvedClassInfo>()

    // 1️⃣ 如果是 FQN，直接查
    if (name.contains(".")) {
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(name, scope)

        if (psiClass != null) {
            result.add(psiClass.toResolvedInfo())
        }
        return result
    }

    // 2️⃣ Simple name 查询
    val cache = PsiShortNamesCache.getInstance(project)
    val classes = cache.getClassesByName(name, scope)
    for (psiClass in classes) {
        result.add(psiClass.toResolvedInfo())
    }
    return result.distinctBy { it.fqn }
}

object ResolveClassNameTool: Tool<ResolveClassNameOutput> {

    const val DEFAULT_LIMIT = 100

    val SPEC = ToolSpecification.builder()
        .name("ResolveClassName")
        .description("""Resolve a Java class or interface name to its fully qualified name(s) (FQN).

Input can be:
- A simple class name, e.g. `UserService`
- A fully qualified name, e.g. `com.example.UserService`

Behavior:
- If a fully qualified name is provided, verify its existence
- If a simple name is provided, search for all matching types
- Return all matching FQNs found in:
  - Project source
  - Project dependencies (including external jars)
- Results include classes, interfaces, enums, and annotation types
- Matching is case-sensitive
- Do not perform fuzzy or partial package matching
- If no match is found, return an empty array
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("name", "Simple class name or fully qualified name (FQN), e.g. 'UserService' or 'com.example.UserService'.")
            .required("name")
            .build())
        .build()

    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }

    override fun getExecuteFunc(): KFunction<ToolCallResult<ResolveClassNameOutput>> {
        return ::execute
    }

    override fun renderResultForAssistant(output: ResolveClassNameOutput): String {
        if (output.results.isEmpty()) {
            return "No matching classes found."
        }
        
        val sb = StringBuilder()
        sb.appendLine("Found ${output.results.size} matching class(es):")
        sb.appendLine()
        
        for (classInfo in output.results) {
            sb.appendLine("- **${classInfo.fqn}**")
            sb.appendLine("  - Type: ${classInfo.type}")
            sb.appendLine("  - Source: ${classInfo.source}")
        }
        if (output.truncated) {
            sb.appendLine("\n(Results are truncated. Consider using a more specific name.)")
        }
        
        return sb.toString().trim()
    }

    suspend fun execute(taskState: TaskState, name: String, toolRequest: ToolExecutionRequest): ToolCallResult<ResolveClassNameOutput> {
        val start = System.currentTimeMillis()
        // 使用 readAction 在读取线程中执行 PSI 操作
        val results = readAction {
            resolveClassName(taskState.project, name)
        }
        val truncated = results.size > GlobTool.DEFAULT_LIMIT
        val durationMs = System.currentTimeMillis() - start
        val data = ResolveClassNameOutput(results.take(DEFAULT_LIMIT), truncated, durationMs)

        taskState.emit!!(JarvisSay(
            id = toolRequest.id(),
            type = AgentMessageType.TOOL,
            data = listOf(renderToolSegment(name, taskState, data))
        ))
        val resultForAssistant = renderResultForAssistant(data)
        return ToolCallResult("result", data, resultForAssistant)
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null

        val name = partialArgs["name"]?.value ?: return null
        return renderToolSegment(name, taskState, null)
    }

    private fun renderToolSegment(name: String, taskState: TaskState, output: ResolveClassNameOutput?): ToolSegment {
        val content = output?.let {
            buildString {
                appendLine("Found ${it.results.size} matching class(es) in ${it.durationMs} ms.")
                append(output.results.joinToString("\n", transform = ResolvedClassInfo::fqn))
                if (output.truncated) {
                    append("\n(Results are truncated. Consider using a more specific path or pattern.)")
                }
            }
        }.orEmpty()

        val segment =  ToolSegment(
            name = UiToolName.RESOLVE_CLASS_NAME,
            toolCommand = name,
            toolContent = content,
            params = mapOf(
                "agent_name" to JsonPrimitive(
                    if (taskState.agentId != "default") taskState.agentId else "Jarvis"
                ),
            )
        )
        if (content.isNotBlank()) {
            taskState.historyAiMessage.segments.last()
                .let {
                    if ((it is ToolSegment) && (it.name == UiToolName.RESOLVE_CLASS_NAME)) {
                        taskState.historyAiMessage.segments.removeLast()
                    }
                    taskState.historyAiMessage.segments.add(segment)
                }
        }
        return segment
    }
}