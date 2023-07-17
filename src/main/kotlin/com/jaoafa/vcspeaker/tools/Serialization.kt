package com.jaoafa.vcspeaker.tools

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import java.io.File

inline fun <reified T> File.readOrCreateAs(
    strategy: SerializationStrategy<T>,
    context: T,
    deserializer: String.() -> T
) = if (!exists()) {
    createNewFile()
    writeText(Json.encodeToString(strategy, context))
    context
} else deserializer(readText())

inline fun <reified T> File.writeAs(strategy: SerializationStrategy<T>, context: T) =
    writeText(Json.encodeToString(strategy, context))