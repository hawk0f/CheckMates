package dev.hawk0f.checkmates.net.lichess

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

fun JsonObject.stringAt(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.objectAt(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.longAt(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

fun JsonObject.intAt(key: String): Int? = longAt(key)?.toInt()

fun JsonObject.boolAt(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

fun JsonObject.typeName(): String? = stringAt("type")
