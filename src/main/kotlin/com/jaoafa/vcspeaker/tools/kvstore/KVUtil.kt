package com.jaoafa.vcspeaker.tools.kvstore

import com.jaoafa.vcspeaker.database.tables.KVTable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

object KVUtil {
    fun <T> get(key: String, serializer: KSerializer<T>): T? = transaction {
        KVTable
            .selectAll()
            .where { KVTable.key eq key }
            .singleOrNull()
            ?.let { Json.decodeFromString(serializer, it[KVTable.value]) }
    }

    fun <T> set(key: String, value: T, serializer: KSerializer<T>) = transaction {
        KVTable.upsert {
            it[KVTable.key] = key
            it[KVTable.value] = Json.encodeToString(serializer, value)
        }
    }

    fun delete(key: String) = transaction {
        KVTable.deleteWhere { KVTable.key eq key }
    }

    inline fun <reified T> get(key: String): T? = get(key, serializer<T>())
    inline fun <reified T> set(key: String, value: T) = set(key, value, serializer<T>())
}


/**
 * Creates a delegate property persisted across launches.
 * The value needs to conform Serializable superclass.
 * 永続化された委譲プロパティを作成する。値は Serializable である必要がある。
 *
 * Example:
 * ```
 * var exampleProperty1 by storedValue<Long>()
 * var exampleProperty2 by storedValue<String>(key = "customKey")
 * ```
 *
 * @param key Optionally specifies the key used to store the value in the DB.
 */
inline fun <reified T> storedValue(key: String? = null): KVProperty<T> =
    KVProperty(serializer(), key)


/**
 * Creates a delegate property persisted across launches, with a default value.
 * The value needs to conform Serializable superclass.
 * Guarantees the retrieved value to be non-null.
 * 永続化された委譲プロパティを作成する。値は Serializable である必要がある。
 * 値が null でないことを保証する。
 *
 * Example:
 * ```
 * var exampleProperty1 by storedValue<String>("defaultValue")
 * ```
 *
 * @param defaultValue The value to fall-back when the entry is non-existent.
 * @param key Optionally specifies the key used to store the value in the DB.
 */
inline fun <reified T> storedValue(defaultValue: T, key: String? = null): KVPropertyWithDefault<T> =
    KVPropertyWithDefault(serializer(), key, defaultValue)
