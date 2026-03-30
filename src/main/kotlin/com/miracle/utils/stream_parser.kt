package com.miracle.utils

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.StringReader

/**
 * JSON 字段提取结果
 */
data class JsonField(
    /** 字段名 */
    val name: String,
    /** 字段值 */
    val value: String,
    /** 值是否完整 */
    val isComplete: Boolean
)

/**
 * 从 JSON 字符串中提取字段（支持JSON 不完整）
 */
object JsonFieldExtractor {

    private val jsonFactory = JsonFactory()

    /**
     * 从 JSON 字符串中提取所有字段信息，支持不完整的 JSON
     * @param jsonChunk JSON 字符串片段
     * @return 字段名到 JsonField 的映射
     */
    fun extractFields(jsonChunk: String): Map<String, JsonField> {
        val results = mutableListOf<JsonField>()
        if (jsonChunk.isBlank()) {
            return results.associateBy { it.name }
        }

        try {
            val parser = jsonFactory.createParser(StringReader(jsonChunk))
            parser.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)

            parser.use { p ->
                if (p.nextToken() != JsonToken.START_OBJECT) {
                    return results.associateBy { it.name }
                }

                while (p.nextToken() != null) {
                    if (p.currentToken != JsonToken.FIELD_NAME) {
                        continue
                    }

                    val fieldName = p.currentName
                    p.nextToken()  // 取字段对应的值

                    var isComplete: Boolean
                    var value: String
                    try {
                        // 如果是对象或数组，跳过子节点
                        if (p.currentToken == JsonToken.START_OBJECT || p.currentToken == JsonToken.START_ARRAY) {
                            val valueStartOffset = p.tokenLocation.charOffset.toInt()
                            p.skipChildren()
                            val valueEndOffset = p.currentLocation.charOffset.toInt()
                            value = jsonChunk.substring(valueStartOffset, valueEndOffset).trim()
                            isComplete = true
                        } else {
                            // 如果是字符串或其他原始值
                            value = p.text
                            val endOffset = p.currentLocation.charOffset.toInt()
                            if (jsonChunk[endOffset - 1] == '"') {
                                isComplete = true
                            } else {
                                // 如果不是字符串，需要判断值后面出现有,或者}才能算 isComplete = true
                                val remaining = jsonChunk.substring(endOffset).trimStart()
                                isComplete = remaining.startsWith(',') || remaining.startsWith('}')
                            }
                        }
                    } catch (e: Exception) {
                        isComplete = false
                        value = jsonChunk.substring(p.tokenLocation.charOffset.toInt()).trim()
                        if (value.startsWith('"')) value = value.substring(1)
                    }

                    results.add(JsonField(fieldName, value, isComplete))
                }
            }
        } catch (e: Exception) {
            // 出现解析异常时直接返回已提取字段
        }

        return results.associateBy { it.name }
    }
}
