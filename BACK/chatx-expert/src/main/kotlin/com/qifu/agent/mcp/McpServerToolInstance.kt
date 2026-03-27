package com.qifu.agent.mcp

import com.intellij.openapi.diagnostic.Logger
import com.qifu.agent.AgentMessageType
import com.qifu.agent.JarvisSay
import com.qifu.agent.TaskState
import com.qifu.agent.mcp.McpClientHub.Companion.MCP_SERVER_PREFIX
import com.qifu.agent.mcp.McpClientHub.ConnectedMcpTool
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.agent.tool.*
import com.qifu.utils.JsonField
import com.qifu.utils.getCurrentProject
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
 * 负责将已连接的 MCP 服务工具实例化为 Agent 可用的 Tool 实例
 */
data class McpToolOutput(
    val serverName: String,
    val toolName: String,
    val arguments: JsonObject?,
    val result: String,
    val structuredResult: JsonObject?
)

class McpServerToolInstance(
    descriptor: ConnectedMcpTool,
) : Tool<McpToolOutput> {

    private val serverName = descriptor.serverName
    private val remoteTool = descriptor.tool
    private val remoteServerName = descriptor.remoteServerName
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

    fun extractErrorMessage(content: List<PromptMessageContent>?): String {
        if (content.isNullOrEmpty()) return ""
        return content.firstNotNullOfOrNull { part ->
            when (part) {
                is TextContent -> part.text
                else -> null
            }
        }.orEmpty()
    }

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

        fun canonicalToolName(serverName: String, toolName: String): String {
            return "$MCP_SERVER_PREFIX${serverName}__${toolName}"
        }
    }

    private fun normalizeArguments(arguments: JsonObject?): JsonObject? {
        return normalizeObjectArguments(arguments, remoteTool.inputSchema.properties)
    }

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
