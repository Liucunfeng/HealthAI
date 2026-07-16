package com.example.healthai.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Gson JsonObject 的容错取值扩展，避免模型返回字段缺失/类型不符时崩溃。 */

fun JsonObject.optString(name: String): String {
    if (!has(name) || get(name).isJsonNull) return ""
    return try {
        get(name).asString
    } catch (e: Exception) {
        ""
    }
}

fun JsonObject.optInt(name: String): Int {
    if (!has(name) || get(name).isJsonNull) return 0
    return try {
        get(name).asInt
    } catch (e: Exception) {
        try {
            get(name).asFloat.toInt()
        } catch (e2: Exception) {
            0
        }
    }
}

fun JsonObject.optFloat(name: String): Float {
    if (!has(name) || get(name).isJsonNull) return 0f
    return try {
        get(name).asFloat
    } catch (e: Exception) {
        0f
    }
}

fun JsonObject.optArray(name: String): List<JsonElement> {
    if (!has(name) || !get(name).isJsonArray) return emptyList()
    return getAsJsonArray(name).toList()
}
