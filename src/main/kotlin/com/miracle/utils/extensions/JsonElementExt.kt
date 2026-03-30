package com.miracle.utils.extensions

import kotlinx.serialization.json.*

/**
 * 将 [JsonElement] 安全转换为字符串，如果无法转换则返回 null。
 * 对于 [JsonPrimitive] 直接返回内容，对于 [JsonArray] 拼接所有原始元素内容。
 *
 * @return 转换后的字符串，转换失败返回 null
 */
fun JsonElement.asStringOrNull(): String? =
    when (this) {
        is JsonPrimitive -> this.contentOrNull
        is JsonArray -> this.joinToString("") {
            (it as? JsonPrimitive)?.contentOrNull ?: ""
        }
        else -> null
    }

/**
 * 将 [JsonElement] 安全转换为布尔值，如果无法转换则返回 null。
 * 对于 [JsonArray] 取第一个元素进行转换。
 *
 * @return 转换后的布尔值，转换失败返回 null
 */
fun JsonElement.asBooleanOrNull(): Boolean? =
    when (this) {
        is JsonPrimitive -> this.booleanOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.booleanOrNull
        else -> null
    }

/**
 * 将 [JsonElement] 安全转换为整数，如果无法转换则返回 null。
 * 对于 [JsonArray] 取第一个元素进行转换。
 *
 * @return 转换后的整数，转换失败返回 null
 */
fun JsonElement.asIntOrNull(): Int? =
    when (this) {
        is JsonPrimitive -> this.intOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.intOrNull
        else -> null
    }

/**
 * 将 [JsonElement] 安全转换为长整数，如果无法转换则返回 null。
 * 对于 [JsonArray] 取第一个元素进行转换。
 *
 * @return 转换后的长整数，转换失败返回 null
 */
fun JsonElement.asLongOrNull(): Long? =
    when (this) {
        is JsonPrimitive -> this.longOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.longOrNull
        else -> null
    }

/**
 * 将 [JsonElement] 安全转换为单精度浮点数，如果无法转换则返回 null。
 * 对于 [JsonArray] 取第一个元素进行转换。
 *
 * @return 转换后的单精度浮点数，转换失败返回 null
 */
fun JsonElement.asFloatOrNull(): Float? =
    when (this) {
        is JsonPrimitive -> this.floatOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.floatOrNull
        else -> null
    }

/**
 * 将 [JsonElement] 安全转换为双精度浮点数，如果无法转换则返回 null。
 * 对于 [JsonArray] 取第一个元素进行转换。
 *
 * @return 转换后的双精度浮点数，转换失败返回 null
 */
fun JsonElement.asDoubleOrNull(): Double? =
    when (this) {
        is JsonPrimitive -> this.doubleOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.doubleOrNull
        else -> null
    }
