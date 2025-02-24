package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.tools.writeAs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

@Serializable
data class TypedStore<T>(
    val version: Int,
    val list: List<T>
)

@Serializable
data class AnyStore(
    val version: Int,
    val list: JsonElement
)

open class StoreStruct<T>(
    path: String,
    private val serializer: KSerializer<T>,
    deserializer: String.() -> TypedStore<T>, // To avoid type inference error. DO NOT REMOVE.
    private val version: Int = 0,
    private val migrators: Map<Int, (File) -> Unit> = emptyMap()
) {
    private val logger = KotlinLogging.logger {}

    val file = File(path)

    var data: MutableList<T> = kotlin.run {
        runMigration()

        file.readOrCreateAs(
            TypedStore.serializer(serializer),
            TypedStore(version, mutableListOf()),
            deserializer
        ).list.toMutableList()
    }

    fun create(element: T): T {
        data.add(element)
        write()

        return element
    }

    fun remove(element: T): Boolean {
        val result = data.remove(element)
        write()

        return result
    }

    fun replace(from: T, to: T): T {
        with(data) {
            remove(from)
            add(to)
        }
        write()

        return to
    }

    fun write() {
        file.writeAs(TypedStore.serializer(serializer), TypedStore(version, this.data))
    }

    private fun runMigration() {
        if (!file.exists()) return

        val fileVersion = try {
            Json.decodeFromString<AnyStore>(file.readText()).version
        } catch (_: Exception) {
            0
        }

        migrators.toSortedMap().forEach { (index, migrator) ->
            if (index <= fileVersion) return@forEach

            logger.info { "Running migration to v$index for ${file.name}" }
            migrator(file)
        }
    }
}