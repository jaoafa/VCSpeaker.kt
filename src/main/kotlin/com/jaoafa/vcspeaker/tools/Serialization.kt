package com.jaoafa.vcspeaker.tools

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

inline fun <reified T> File.readAs() = Json.decodeFromString<T>(readText())

inline fun <reified T> File.readOrCreateAs(strategy: SerializationStrategy<T>, context: T) = let {
    if (!it.exists()) {
        it.createNewFile()
        it.writeText(Json.encodeToString(strategy, context))
        context
    } else readAs<T>()
}

inline fun <reified T> File.writeAs(strategy: SerializationStrategy<T>, context: T) =
    writeText(Json.encodeToString(strategy, context))