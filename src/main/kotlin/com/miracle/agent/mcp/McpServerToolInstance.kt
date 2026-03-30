package com.miracle.agent.mcp

import com.intellij.openapi.diagnostic.Logger
import com.miracle.agent.AgentMessageType
import com.miracle.agent.JarvisSay
import com.miracle.agent.TaskState
import com.miracle.agent.mcp.McpClientHub.Companion.MCP_SERVER_PREFIX
import com.miracle.agent.mcp.McpClientHub.ConnectedMcpTool
import com.miracle.agent.parser.ToolSegment
import com.miracle.agent.parser.UiToolName
import com.miracle.agent.tool.*
import com.miracle.utils.JsonField
import com.miracle.utils.getCurrentProject
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.*
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.PromptMessageContent
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.math.BigDecimal
import kotlin.reflect.KFunction

/**
 * MCP 工具调用的输出结果
 *
 * @property serverName MCP 服务器名称
 * @property toolName 工具名称
 * @property arguments 调用参数
 * @property result 文本形式的结果
 * @property structuredResult 结构化 JSON 结果
 */
data class McpToolOutput(
    val serverName: String,
    val toolName: String,
    val arguments: JsonObject?,
    val result: String,
    val structuredResult: JsonObject?
)

/**
 * 负责将已连接的 MCP 服务工具实例化为 Agent 可用的 Tool 实例，
 * 包含工具规格构建、参数校验、参数归一化、执行调用和结果渲染等完整生命周期。
 *
 * @param descriptor 已连接的 MCP 工具描述信息
 */
class McpServerToolInstance(
    descriptor: ConnectedMcpTool,
) : Tool<McpToolOutput> {

    /** MCP 服务器名称 */
    private val serverName = descriptor.serverName
    /** MCP 远程工具定义 */
    private val remoteTool = descriptor.tool
    /** 远程服务器名称 */
    private val remoteServerName = descriptor.remoteServerName
    /** LangChain4j 工具规格 */
    private val toolSpecification: ToolSpecification = buildSpecification(descriptor)
    private val LOG = Logger.getInstance(McpServerToolInstance::class.java)


    init {
        //注册MCP Tool
        McpPromptIntegration.toolsNameSet.add(canonicalToolName(serverName, remoteTool.name))
    }

    override fun getToolSpecification(): ToolSpecification = toolSpecification

    override fun getExecuteFunc(): KFunction<ToolCallResult<McpToolOutput>> = this::execute

    override fun renderResultForAssistant(output: McpToolOutput): String {
        return buildResultText(output)
    }

    /**
     * 从 MCP 响应内容中提取错误消息文本
     *
     * @param content MCP 响应内容列表
     * @return 错误消息文本，无内容时返回空字符串
     */
    fun extractErrorMessage(content: List<PromptMessageContent>?): String {
        if (content.isNullOrEmpty()) return ""
        return content.firstNotNullOfOrNull { part ->
            when (part) {
                is TextContent -> part.text
                else -> null
            }
        }.orEmpty()
    }

    /**
     * 将 MCP 响应内容列表渲染为文本字符串
     *
     * @param content MCP 响应内容列表
     * @return 拼接后的文本内容
     */
    fun renderContent(content: List<PromptMessageContent>?): String {
        if (content.isNullOrEmpty()) {
            return ""
        }
        return content.joinToString(separator = "\n") { part ->
            when (part) {
                is TextContent -> part.text.orEmpty()
                else -> part.toString()
            }
        }.trim()
    }

    /**
     * 构建工具调用结果的文本表示，优先使用结构化结果
     *
     * @param output 工具输出结果
     * @return 格式化后的结果文本
     */
    fun buildResultText(
        output: McpToolOutput,
    ): String {
        val structuredText = output.structuredResult?.takeIf { it.isNotEmpty() }?.let { formatJsonObject(it) }
        val resultText = output.result.trim().ifEmpty { "(empty)" }
        return buildString {
            val resultMsg = if (structuredText != null && structuredText.isNotBlank()) {
                structuredText
            } else {
                resultText
            }
            append("Response:\n$resultMsg")
        }
    }

    private fun formatJsonObject(jsonObject: JsonObject): String {
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), jsonObject)
    }

    /**
     * 执行 MCP 工具调用，向远程服务器发送请求并返回结果
     *
     * @param originParams 原始调用参数
     * @param taskState 任务状态，用于发送进度事件
     * @param toolRequest 工具执行请求
     * @return 工具调用结果
     * @throws ToolExecutionException 执行失败时抛出
     */
    @Suppress("unused")
    suspend fun execute(
        originParams: JsonObject? = null,
        taskState: TaskState,
        toolRequest: ToolExecutionRequest
    ): ToolCallResult<McpToolOutput> {
        val normalizedArguments = normalizeArguments(originParams)
        return try {
            LOG.info("Executing MCP tool: $serverName/${remoteTool.name}")
            val project = getCurrentProject()
                ?: throw ToolExecutionException("No active project available for MCP tool execution.")
            val hub = McpClientHub.getInstance(project).apply { ensureInitialized() }
            val normalizedArguments = normalizedArguments ?: JsonObject(emptyMap())
            val callResult = hub.withConnectedClient(serverName) { client ->
                client.callTool(
                    CallToolRequest(
                        name = remoteTool.name,
                        arguments = normalizedArguments
                    )
                )
            }
            if (callResult?.isError == true) {
                val message = extractErrorMessage(callResult.content)
                throw ToolExecutionException(
                    message.ifBlank { "MCP tool '${remoteTool.name}' returned an error response." }
                )
            }
            val data = McpToolOutput(
                serverName = serverName,
                toolName = remoteTool.name,
                arguments = normalizedArguments,
                result = renderContent(callResult?.content),
                structuredResult = callResult?.structuredContent
            )
            LOG.info("MCP tool execution completed successfully")
            taskState.emit!!(
                JarvisSay(
                    id = toolRequest.id(),
                    type = AgentMessageType.TOOL,
                    data = listOf(
                        ToolSegment(
                            name = UiToolName.MCP_TOOL_RESPONSE,
                            toolCommand = remoteTool.name,
                            toolContent = buildResultText(
                                data
                            ),
                            params = mapOf(
                                "server_name" to JsonPrimitive(serverName),
                                "tool_name" to JsonPrimitive(remoteTool.name),
                            )
                        )
                    )
                )
            )
            ToolCallResult(
                type = "result",
                data = data,
                resultForAssistant = renderResultForAssistant(data)
            )
        } catch (e: Exception) {
            val errorMessage = "Failed to execute MCP tool $serverName/$remoteTool.name: ${e.message}"
            LOG.warn(errorMessage, e)
            throw ToolExecutionException(errorMessage)
        }
    }

    override fun getMcpMetaInfo(): Map<String, String> {
        val metaInfo = mutableMapOf<String, String>()
        metaInfo[Tool.MCP_SERVER_NAME_KEY] = serverName
        metaInfo[Tool.MCP_TOOL_NAME_KEY] = remoteTool.name
        metaInfo[Tool.MCP_REMOTE_SERVER_NAME_KEY] = remoteServerName
        return metaInfo
    }

    override suspend fun handlePartialBlock(
        toolRequestId: String,
        partialArgs: Map<String, JsonField>,
        taskState: TaskState,
        isPartial: Boolean
    ): ToolSegment? {
        if (isPartial) return null

        val params = mutableMapOf<String, JsonElement>(
            "server_name" to JsonPrimitive(serverName),
            "tool_name" to JsonPrimitive(remoteTool.name)
        )
        remoteTool.description?.takeIf { it.isNotBlank() }?.let {
            params["tool_description"] = JsonPrimitive(it)
        }
        val originParams = partialArgs.values.associate { it.name to it.value }
        return ToolSegment(
            name = UiToolName.MCP_TOOL, // 使用现有的 UI 工具名称
            toolCommand = remoteTool.name,
            params = params,
            toolContent = PRETTY_JSON.encodeToString(originParams),
        )
    }

    /**
     * 根据已连接的 MCP 工具描述构建 LangChain4j ToolSpecification，
     * 将 MCP 工具的 inputSchema 转换为 JSON Schema 定义
     *
     * @param descriptor 已连接的 MCP 工具描述
     * @return LangChain4j 工具规格
     */
    fun buildSpecification(descriptor: McpClientHub.ConnectedMcpTool): ToolSpecification {
        val tool = descriptor.tool
        val canonicalName = canonicalToolName(descriptor.serverName, tool.name)
        val description = tool.description?.takeIf { it.isNotBlank() } ?: "This tool has No description."
        val builder = JsonObjectSchema.builder()
        tool.inputSchema.properties.forEach { (propertyName, propertySchema) ->
            builder.addProperty(propertyName, buildJsonSchemaElement(propertySchema))
        }

        builder.required(tool.inputSchema.required ?: emptyList())

        return ToolSpecification.builder()
            .name(canonicalName)
            .description(description)
            .parameters(builder.build())
            .build()
    }

    override suspend fun validateInput(input: JsonElement, taskState: TaskState) {
        val required = toolSpecification.parameters().required()?.takeIf { it.isNotEmpty() }
        //判断必填参数数量是否足够
        if (input.jsonObject.values.size < (required?.size ?: 0)) {
            throw ToolParameterException("Input parameters are incomplete.")
        }
        (input as JsonObject).let {
            //判断必填字段是否存在
            required?.forEach { requiredParam ->
                if (!(it.containsKey(requiredParam))) {
                    throw MissingToolParameterException(getToolSpecification().name(), requiredParam)
                }
            }
        }
    }

    companion object {
        private val PRETTY_JSON = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    /**
     * 将 MCP 工具名称转换为规范的 Agent 工具名称格式（mcp__serverName__toolName）
     *
     * @param serverName 服务器名称
     * @param toolName 工具名称
     * @return 规范化后的工具名称
     */
    fun canonicalToolName(serverName: String, toolName: String): String {
            return "$MCP_SERVER_PREFIX${serverName}__${toolName}"
        }
    }

    /**
     * 根据工具的 inputSchema 对调用参数进行类型归一化处理，
     * 将字符串形式的参数值强制转换为 schema 声明的类型
     *
     * @param arguments 原始调用参数
     * @return 归一化后的参数，无需变更时返回原始对象
     */
    private fun normalizeArguments(arguments: JsonObject?): JsonObject? {
        return normalizeObjectArguments(arguments, remoteTool.inputSchema.properties)
    }

    /**
     * 对 JSON 对象中的每个字段根据 schema 属性定义进行归一化处理
     *
     * @param arguments 待归一化的 JSON 对象
     * @param schemaProperties schema 中的属性定义
     * @return 归一化后的 JSON 对象
     */
    private fun normalizeObjectArguments(
        arguments: JsonObject?,
        schemaProperties: JsonObject?
    ): JsonObject? {
        arguments ?: return null
        schemaProperties ?: return arguments
        if (schemaProperties.isEmpty()) return arguments

        var changed = false
        val normalized = LinkedHashMap<String, JsonElement>(arguments.size)
        arguments.forEach { (name, value) ->
            val schema = schemaProperties[name]
            val normalizedValue = schema?.let { normalizeValueAgainstSchema(value, it) } ?: value
            if (normalizedValue != value) {
                changed = true
            }
            normalized[name] = normalizedValue
        }
        return if (changed) JsonObject(normalized) else arguments
    }

    /**
     * 根据 schema 类型定义对单个 JSON 值进行归一化
     *
     * @param value 待归一化的 JSON 值
     * @param rawSchema 该字段的 schema 定义
     * @return 归一化后的 JSON 值
     */
    private fun normalizeValueAgainstSchema(value: JsonElement, rawSchema: JsonElement): JsonElement {
        val schemaObject = rawSchema.asJsonObjectOrNull() ?: return value
        val types = extractSchemaTypes(schemaObject)

        if (value is JsonNull) return value
        if (SchemaType.NULL in types &&
            value is JsonPrimitive &&
            value.isString &&
            value.content.trim().equals("null", ignoreCase = true)
        ) {
            return JsonNull
        }

        return when {
            SchemaType.OBJECT in types -> normalizeObjectType(value, schemaObject)
            SchemaType.ARRAY in types -> normalizeArrayType(value, schemaObject)
            SchemaType.BOOLEAN in types -> coerceBoolean(value)
            SchemaType.INTEGER in types -> coerceInteger(value)
            SchemaType.NUMBER in types -> coerceNumber(value)
            SchemaType.STRING in types -> coerceString(value)
            else -> value
        }
    }

    private fun normalizeObjectType(value: JsonElement, schemaObject: JsonObject): JsonElement {
        val existingObject = value as? JsonObject
        val parsedObject = existingObject ?: (value as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.let { parseJsonElementOrNull(it.content.trim()) as? JsonObject }
            ?: return value

        val nestedSchema = schemaObject["properties"].asJsonObjectOrNull()
        val normalized = normalizeObjectArguments(parsedObject, nestedSchema) ?: parsedObject
        return if (existingObject != null && normalized == existingObject) value else normalized
    }

    private fun normalizeArrayType(value: JsonElement, schemaObject: JsonObject): JsonElement {
        val existingArray = value as? JsonArray
        val parsedArray = existingArray ?: (value as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.let { parseJsonElementOrNull(it.content.trim()) as? JsonArray }
            ?: return value

        val itemsSchema = resolveItemsSchema(schemaObject["items"])
        var changed = existingArray == null
        val normalizedItems: List<JsonElement> = if (itemsSchema != null) {
            parsedArray.map { element ->
                val normalizedElement = normalizeValueAgainstSchema(element, itemsSchema)
                if (normalizedElement != element) {
                    changed = true
                }
                normalizedElement
            }
        } else {
            parsedArray.map { it }
        }

        return if (!changed && existingArray != null) value else JsonArray(normalizedItems)
    }

    private fun coerceBoolean(value: JsonElement): JsonElement {
        val primitive = value as? JsonPrimitive ?: return value
        val candidate = primitive.content.trim().lowercase()
        val coerced = when (candidate) {
            "true", "1" -> true
            "false", "0" -> false
            else -> return value
        }
        return JsonPrimitive(coerced)
    }

    private fun coerceInteger(value: JsonElement): JsonElement {
        val primitive = value as? JsonPrimitive ?: return value
        if (!primitive.isString) return value

        val normalizedNumber = primitive.content.trim().toBigDecimalOrNull() ?: return value
        val integerCandidate = normalizedNumber.stripTrailingZeros()
        if (integerCandidate.scale() > 0) return value

        return parseJsonNumber(integerCandidate.toPlainString()) ?: value
    }

    private fun coerceNumber(value: JsonElement): JsonElement {
        val primitive = value as? JsonPrimitive ?: return value
        if (!primitive.isString) return value

        val normalizedNumber = primitive.content.trim().toBigDecimalOrNull() ?: return value
        return parseJsonNumber(normalizedNumber.toPlainString()) ?: value
    }

    private fun coerceString(value: JsonElement): JsonElement {
        val primitive = value as? JsonPrimitive ?: return value
        if (primitive.isString) return value
        return JsonPrimitive(primitive.content)
    }

    private fun parseJsonNumber(source: String): JsonElement? = runCatching {
        Json.parseToJsonElement(source)
    }.getOrNull()?.takeIf { it is JsonPrimitive && !it.isString }

    private fun parseJsonElementOrNull(source: String): JsonElement? = runCatching {
        Json.parseToJsonElement(source)
    }.getOrNull()

    private fun resolveItemsSchema(itemsElement: JsonElement?): JsonElement? {
        return when (itemsElement) {
            null, JsonNull -> null
            is JsonArray -> itemsElement.firstOrNull()
            else -> itemsElement
        }
    }

    /**
     * 将 JSON Schema 元素转换为 LangChain4j 的 JsonSchemaElement
     *
     * @param schema JSON Schema 元素
     * @return 对应的 JsonSchemaElement 实例
     */
    private fun buildJsonSchemaElement(schema: JsonElement): JsonSchemaElement {
        val schemaObject = schema.asJsonObjectOrNull() ?: return JsonStringSchema()
        val description = schemaObject["description"]?.jsonPrimitive?.contentOrNull
        val enumValues = schemaObject["enum"]
            .asJsonArrayOrNull()
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.takeIf { it.isNotEmpty() }
        val types = extractSchemaTypes(schemaObject)

        if (!enumValues.isNullOrEmpty()) {
            return JsonEnumSchema.builder()
                .apply { description?.let { description(it) } }
                .enumValues(enumValues)
                .build()
        }

        return when {
            SchemaType.OBJECT in types -> buildNestedObjectSchema(schemaObject, description)
            SchemaType.ARRAY in types -> buildArraySchema(schemaObject, description)
            SchemaType.INTEGER in types -> buildIntegerSchema(description)
            SchemaType.NUMBER in types -> buildNumberSchema(description)
            SchemaType.BOOLEAN in types -> buildBooleanSchema(description)
            else -> buildStringSchema(description)
        }
    }

    private fun buildNestedObjectSchema(schemaObject: JsonObject, description: String?): JsonSchemaElement {
        val builder = JsonObjectSchema.builder()
        description?.let { builder.description(it) }
        schemaObject["properties"].asJsonObjectOrNull()?.forEach { (name, nestedSchema) ->
            builder.addProperty(name, buildJsonSchemaElement(nestedSchema))
        }
        val required = schemaObject["required"]
            .asJsonArrayOrNull()
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.takeIf { it.isNotEmpty() }
        if (required != null) {
            builder.required(required)
        }
        return builder.build()
    }

    private fun buildArraySchema(schemaObject: JsonObject, description: String?): JsonSchemaElement {
        val builder = JsonArraySchema.builder()
        description?.let { builder.description(it) }
        val itemsSchema = resolveItemsSchema(schemaObject["items"])
        val itemElement = itemsSchema?.let { buildJsonSchemaElement(it) } ?: buildStringSchema(null)
        builder.items(itemElement)
        return builder.build()
    }

    private fun buildStringSchema(description: String?): JsonSchemaElement {
        return description?.let { JsonStringSchema.builder().description(it).build() } ?: JsonStringSchema()
    }

    private fun buildIntegerSchema(description: String?): JsonSchemaElement {
        return description?.let { JsonIntegerSchema.builder().description(it).build() } ?: JsonIntegerSchema()
    }

    private fun buildNumberSchema(description: String?): JsonSchemaElement {
        return description?.let { JsonNumberSchema.builder().description(it).build() } ?: JsonNumberSchema()
    }

    private fun buildBooleanSchema(description: String?): JsonSchemaElement {
        return description?.let { JsonBooleanSchema.builder().description(it).build() } ?: JsonBooleanSchema()
    }

    /**
     * 从 schema 对象中提取类型集合，支持字符串和数组形式的 type 定义，
     * 无法从 type 推断时尝试根据 properties/items 字段推断
     *
     * @param schemaObject schema JSON 对象
     * @return 推断出的类型集合
     */
    private fun extractSchemaTypes(schemaObject: JsonObject): Set<SchemaType> {
        val typeElement = schemaObject["type"]
        val explicitTypes = when (typeElement) {
            is JsonPrimitive -> schemaTypeFromName(typeElement.contentOrNull)?.let { setOf(it) } ?: emptySet()
            is JsonArray -> typeElement.mapNotNull { schemaTypeFromName(it.jsonPrimitive.contentOrNull) }.toSet()
            else -> emptySet()
        }
        if (explicitTypes.isNotEmpty()) return explicitTypes

        return when {
            schemaObject.containsKey("properties") -> setOf(SchemaType.OBJECT)
            schemaObject.containsKey("items") -> setOf(SchemaType.ARRAY)
            else -> emptySet()
        }
    }

    private fun schemaTypeFromName(name: String?): SchemaType? {
        return when (name?.lowercase()) {
            "string" -> SchemaType.STRING
            "number" -> SchemaType.NUMBER
            "integer" -> SchemaType.INTEGER
            "boolean" -> SchemaType.BOOLEAN
            "array" -> SchemaType.ARRAY
            "object" -> SchemaType.OBJECT
            "null" -> SchemaType.NULL
            else -> null
        }
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray

    /** JSON Schema 类型枚举，用于类型推断和归一化 */
    private enum class SchemaType {
        STRING,
        NUMBER,
        INTEGER,
        BOOLEAN,
        ARRAY,
        OBJECT,
        NULL
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching {
        BigDecimal(this)
    }.getOrNull()
}
