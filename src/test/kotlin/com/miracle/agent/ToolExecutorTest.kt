package com.miracle.agent

import com.miracle.agent.tool.EditTool
import com.miracle.agent.tool.ToolParameterException
import com.miracle.utils.extensions.asBooleanOrNull
import com.miracle.utils.extensions.asDoubleOrNull
import com.miracle.utils.extensions.asFloatOrNull
import com.miracle.utils.extensions.asIntOrNull
import com.miracle.utils.extensions.asLongOrNull
import com.miracle.utils.extensions.asStringOrNull
import dev.langchain4j.agent.tool.ToolExecutionRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializerOrNull
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.test.Test

class ToolExecutorTest {

    @Test
    fun testConvertToolArgs() {
        val json = Json {
            ignoreUnknownKeys = true
        }
        val args = mutableMapOf<KParameter, Any?>()

        val argStr = "{\"file_path\": \"D:/idea_work/lowcode/cloud-agent/pyproject.toml\", \"old_string\": [\"claude-agent-sdk>=0.1.13\"], \"new_string\": [\"claude-agent-sdk>=0.1.14\"], \"replace_all\": false}"
        val jsonObj = Json.parseToJsonElement(argStr).jsonObject
        val func = EditTool.getExecuteFunc()
        val params = func.parameters.filter { it.kind == KParameter.Kind.VALUE }
        params.forEach { param ->
            val paramName = param.name ?: throw ToolParameterException("Unnamed parameter")
            val paramType = param.type.classifier as? KClass<*> ?: throw ToolParameterException("Unknown parameter type: ${param.type} for param $paramName")
            if (paramType == TaskState::class) {
                args[param] = null
                return@forEach
            }
            if (paramType == ToolExecutionRequest::class) {
                args[param] = null
                return@forEach
            }
            if (paramType == JsonObject::class && paramName == "originParams") {
                args[param] = jsonObj
            }

            // 检查参数是否在JSON中提供
            val jsonElement = jsonObj[paramName]
            if (jsonElement != null) {
                // 参数在JSON中提供，进行解析
                try {
                    val value = when (paramType) {
                        String::class -> jsonElement.asStringOrNull()
                        Int::class -> jsonElement.asIntOrNull()
                        Boolean::class -> jsonElement.asBooleanOrNull()
                        Double::class -> jsonElement.asDoubleOrNull()
                        Long::class -> jsonElement.asLongOrNull()
                        Float::class -> jsonElement.asFloatOrNull()
                        else -> {
                            // 复杂类型必须标记kotlin的@Serializable注解
                            val serializer = serializerOrNull(param.type)
                                ?: throw ToolParameterException("Cannot get serializer for parameter: ${param.name}")
                            if (jsonElement is JsonPrimitive && jsonElement.isString) {
                                val innerJson = Json.parseToJsonElement(jsonElement.content)
                                json.decodeFromJsonElement(serializer, innerJson)
                            } else {
                                json.decodeFromJsonElement(serializer, jsonElement)
                            }
                        }
                    }
                    args[param] = value
                } catch (_: Exception) {
                    throw ToolParameterException("Invalid parameter value for param $paramName: ${jsonElement.jsonPrimitive.content}")
                }
            } else if (param.isOptional) {
                // 参数是可选的（有默认值），不提供则使用默认值
                // 不添加到args中，Kotlin反射会自动使用默认值
            } else {
                throw ToolParameterException("Missing required parameter: $paramName")
            }
        }
        println(args)
    }
}