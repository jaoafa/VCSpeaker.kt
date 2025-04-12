package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.tools.writeAs
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import kotlin.system.exitProcess

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

/**
 * StoreStructは、配列データを保存するための構造体です。
 *
 * @param path データを保存するパス
 * @param serializer データの Serializer
 * @param deserializer データの Deserializer; 文字列を受け取り、[TypedStore] を返す関数です
 * @param version データ構造のバージョン
 * @param migrators Migrator 関数; Key をバージョンとし, v (Key - 1) のデータを v Key に移行します
 * @param auditor Auditor 関数; null の場合は何も変更されません。null でない場合、初期化時と [StoreStruct.write] 実行時にデータを監査します
 */
open class StoreStruct<T>(
    path: String,
    private val serializer: KSerializer<T>,
    deserializer: String.() -> TypedStore<T>, // To avoid type inference error. DO NOT REMOVE.
    private val version: Int = 0,
    private val migrators: Map<Int, (File) -> Unit> = emptyMap(),
    private val auditor: ((MutableList<T>) -> MutableList<T>)? = null
) {
    private val logger = KotlinLogging.logger {}

    val file = File(path)

    var data: MutableList<T> = kotlin.run {
        runMigration()

        val dataCandidate = file.readOrCreateAs(
            TypedStore.serializer(serializer),
            TypedStore(version, mutableListOf()),
            deserializer
        ).list.toMutableList()

        auditData(dataCandidate).also {
            write(it)
        }
    }

    fun create(element: T): T {
        data.add(element)
        data = auditData(data)

        write()

        return element
    }

    fun remove(element: T): Boolean {
        val result = data.remove(element)
        data = auditData(data)

        write()

        return result
    }

    fun replace(from: T, to: T): T {
        with(data) {
            remove(from)
            add(to)
        }

        data = auditData(data)
        write()

        return to
    }

    fun write(modifiedData: MutableList<T>? = null) {
        file.writeAs(TypedStore.serializer(serializer), TypedStore(version, modifiedData ?: this.data))
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

            val backup = file.readText()

            try {
                migrator(file)
            } catch (exception: Exception) {
                logger.error(exception) { "Failed to migrate ${file.name} to v$index. Rolling back..." }
                file.writeText(backup)

                Sentry.captureException(exception)

                exitProcess(1)
            }
        }
    }

    private fun auditData(dataCandidate: MutableList<T>): MutableList<T> =
        auditor?.let { it(dataCandidate) } ?: dataCandidate
}