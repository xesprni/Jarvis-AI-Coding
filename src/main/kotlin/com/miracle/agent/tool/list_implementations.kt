package com.miracle.agent.tool

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.utils.JsonField
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KFunction

/**
 * 继承者/实现者信息
 */
data class InheritorInfo(
    val fqn: String, // 类的完全限定名
    val type: String, // 类型（class/interface/enum/annotation）
    val source: String, // 来源（project/dependency）
    val isAbstract: Boolean // 是否为抽象类
)

/**
 * 工具输出数据
 */
data class ListImplementationsOutput(
    val targetFqn: String, // 目标类型的完全限定名
    val targetType: String, // 目标类型（class/interface/enum）
    val inheritors: List<InheritorInfo>, // 继承者/实现者列表
    val truncated: Boolean, // 结果是否被截断
    val durationMs: Long // 查询耗时（毫秒）
)

/**
 * 将 PsiClass 转换为 InheritorInfo
 */
fun PsiClass.toInheritorInfo(): InheritorInfo {
    val fqn = this.qualifiedName ?: "<anonymous>"
    
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
    
    val isAbstract = this.hasModifierProperty("abstract")
    
    return InheritorInfo(
        fqn = fqn,
        type = type,
        source = source,
        isAbstract = isAbstract
    )
}

/**
 * 查找给定类型的所有继承者/实现者
 */
fun listImplementations(project: Project, fqn: String): ListImplementationsOutput {
    val scope = GlobalSearchScope.allScope(project)
    
    // 查找目标类
    val targetClass = JavaPsiFacade.getInstance(project)
        .findClass(fqn, scope)
    
    // 如果目标类不存在,返回空结果
    if (targetClass == null) {
        return ListImplementationsOutput(
            targetFqn = fqn,
            targetType = "unknown",
            inheritors = emptyList(),
            truncated = false,
            durationMs = 0
        )
    }
    
    val targetType = when {
        targetClass.isInterface -> "interface"
        targetClass.isEnum -> "enum"
        targetClass.isAnnotationType -> "annotation"
        else -> "class"
    }
    
    // 查找所有继承者
    val inheritors = ClassInheritorsSearch.search(targetClass, scope, true)
        .findAll()
        .filter { psiClass ->
            // 排除匿名类和合成类
            psiClass.qualifiedName != null && 
            !psiClass.name.isNullOrEmpty()
        }
        .map { it.toInheritorInfo() }
        .distinctBy { it.fqn }
        .sortedWith(
            compareBy<InheritorInfo> { it.source } // project 优先
                .thenBy { it.fqn }
        )
    
    return ListImplementationsOutput(
        targetFqn = fqn,
        targetType = targetType,
        inheritors = inheritors,
        truncated = false,
        durationMs = 0
    )
}

/**
 * 查找给定类型的所有继承者/实现者的工具
 */
object ListImplementationsTool : Tool<ListImplementationsOutput> {
    
    const val DEFAULT_LIMIT = 100 // 默认返回结果数量上限
    
    val SPEC = ToolSpecification.builder() // 工具规格定义，供模型识别和调用
        .name("ListImplementations")
        .description("""List all known inheritors of a given Java type.

Input must be the fully qualified name (FQN) of a Java interface or class.

Behavior:
- If the target type is an interface:
  - Return all classes and interfaces that implement or extend it (directly or indirectly)
- If the target type is a class:
  - Return all subclasses (directly or indirectly)
- Include abstract classes and interfaces
- Exclude anonymous and synthetic classes
- Search includes project source and dependencies
- Return results as structured data
- Do not return duplicates
- If the target type does not exist or has no inheritors, return an empty array

Notes:
- Matching is case-sensitive
- Inner classes use standard FQN format (e.g., com.example.Outer.Inner)
""")
        .parameters(JsonObjectSchema.builder()
            .addStringProperty("fqn", "The fully qualified name (FQN) of the interface or class.")
            .required("fqn")
            .build())
        .build()
    
    /**
     * 获取工具规格定义
     * @return 工具规格
     */
    override fun getToolSpecification(): ToolSpecification {
        return SPEC
    }
    
    /**
     * 获取工具执行函数的引用
     * @return 执行函数
     */
    override fun getExecuteFunc(): KFunction<ToolCallResult<ListImplementationsOutput>> {
        return ::execute
    }

    /**
     * 将工具输出结果渲染为给助手的文本
     * @param output 工具输出
     * @return 渲染后的文本
     */
    override fun renderResultForAssistant(output: ListImplementationsOutput): String {
        if (output.inheritors.isEmpty()) {
            return "No inheritors found for ${output.targetFqn}."
        }
        
        val sb = StringBuilder()
        sb.appendLine("Found ${output.inheritors.size} inheritor(s) of ${output.targetFqn} (${output.targetType}):")
        sb.appendLine()
        
        for (inheritor in output.inheritors) {
            sb.append("- **${inheritor.fqn}**")
            val details = mutableListOf<String>()
            details.add(inheritor.type)
            if (inheritor.isAbstract) {
                details.add("abstract")
            }
            details.add(inheritor.source)
            sb.appendLine(" (${details.joinToString(", ")})")
        }
        
        if (output.truncated) {
            sb.appendLine("\n(Results are truncated. Consider using a more specific type.)")
        }
        
        return sb.toString().trim()
    }
    
    /**
     * 执行查找类继承者/实现者操作
     * @param taskState 当前任务状态
     * @param fqn 目标类型的完全限定名
     * @param toolRequest 工具调用请求
     * @return 工具调用结果
     */
    suspend fun execute(
        taskState: TaskState,
        fqn: String,
        toolRequest: ToolExecutionRequest
    ): ToolCallResult<ListImplementationsOutput> {
        val start = System.currentTimeMillis()
        
        // 使用 readAction 在读取线程中执行 PSI 操作
        val result = readAction {
            listImplementations(taskState.project, fqn)
        }
        
        val truncated = result.inheritors.size > DEFAULT_LIMIT
        val data = result.copy(
            inheritors = result.inheritors.take(DEFAULT_LIMIT),
            truncated = truncated,
            durationMs = System.currentTimeMillis() - start
        )
        
        taskState.emit!!(JarvisSay(
            id = toolRequest.id(),
            type = AgentMessageType.TOOL,
            data = listOf(renderToolSegment(fqn, taskState, data))
        ))
        
        val resultForAssistant = renderResultForAssistant(data)
        return ToolCallResult("result", data, resultForAssistant)
    }
    
    /**
     * 处理流式参数块，构建 UI 展示片段
     * @param toolRequestId 工具请求ID
     * @param partialArgs 部分参数
     * @param taskState 当前任务状态
     * @param isPartial 是否为部分参数
     * @return UI 展示片段
     */
    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null
        
        val fqn = partialArgs["fqn"]?.value ?: return null
        return renderToolSegment(fqn, taskState, null)
    }
    
    /**
     * 构建 ListImplementations 工具的 UI 展示片段
     * @param fqn 目标类型的完全限定名
     * @param taskState 当前任务状态
     * @param output 工具输出（可为 null 表示搜索未完成）
     * @return 工具展示片段
     */
    private fun renderToolSegment(
        fqn: String,
        taskState: TaskState,
        output: ListImplementationsOutput?
    ): ToolSegment {
        val content = output?.let {
            buildString {
                if (it.inheritors.isEmpty()) {
                    appendLine("No inheritors found in ${it.durationMs} ms.")
                } else {
                    appendLine("Found ${it.inheritors.size} inheritor(s) in ${it.durationMs} ms:")
                    append(it.inheritors.joinToString("\n") { inheritor ->
                        val tags = mutableListOf<String>()
                        tags.add(inheritor.type)
                        if (inheritor.isAbstract) {
                            tags.add("abstract")
                        }
                        tags.add(inheritor.source)
                        "${inheritor.fqn} (${tags.joinToString(", ")})"
                    })
                    if (it.truncated) {
                        append("\n(Results are truncated. Consider using a more specific type.)")
                    }
                }
            }
        }.orEmpty()
        
        val segment = ToolSegment(
            name = UiToolName.LIST_IMPLEMENTATIONS,
            toolCommand = fqn,
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
                    if ((it is ToolSegment) && (it.name == UiToolName.LIST_IMPLEMENTATIONS)) {
                        taskState.historyAiMessage.segments.removeLast()
                    }
                    taskState.historyAiMessage.segments.add(segment)
                }
        }
        
        return segment
    }
}
