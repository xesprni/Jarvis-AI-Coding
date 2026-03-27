package com.miracle.utils.extensions

import kotlinx.serialization.json.*

fun JsonElement.asStringOrNull(): String? =
    when (this) {
        is JsonPrimitive -> this.contentOrNull
        is JsonArray -> this.joinToString("") {
            (it as? JsonPrimitive)?.contentOrNull ?: ""
        }
        else -> null
    }

fun JsonElement.asBooleanOrNull(): Boolean? =
    when (this) {
        is JsonPrimitive -> this.booleanOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.booleanOrNull
        else -> null
    }

fun JsonElement.asIntOrNull(): Int? =
    when (this) {
        is JsonPrimitive -> this.intOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.intOrNull
        else -> null
    }

fun JsonElement.asLongOrNull(): Long? =
    when (this) {
        is JsonPrimitive -> this.longOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.longOrNull
        else -> null
    }

fun JsonElement.asFloatOrNull(): Float? =
    when (this) {
        is JsonPrimitive -> this.floatOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.floatOrNull
        else -> null
    }

fun JsonElement.asDoubleOrNull(): Double? =
    when (this) {
        is JsonPrimitive -> this.doubleOrNull
        is JsonArray -> (this.firstOrNull() as? JsonPrimitive)?.doubleOrNull
        else -> null
    }
