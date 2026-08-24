package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.tables.GameEntity
import com.jaoafa.vcspeaker.stores.GameStore.LAST_FETCHED_AT_ID
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class GameData(
    val id: Long,
    val name: String,
    val updatedAt: Long,
    override var migrated: Boolean = false
) : DBMigratableData {
    override fun migrate() = transaction {
        GameEntity.new(Snowflake(this@GameData.id)) {
            name = this@GameData.name
        }
        return@transaction
    }
}

/**
 * `applications/detectable` から取得したゲーム一覧のキャッシュ。
 * `updatedAt` は game ID ごとの値ではなく、カタログ全体を最後に再取得した時刻。
 * Discord のゲーム ID は正の値のみのため、[LAST_FETCHED_AT_ID] を負の値の
 * sentinel エントリとして [data] に混在させ、空カタログでも最終取得時刻を保持する。
 */
@Deprecated("Use database instead")
object GameStore : StoreStruct<GameData>(
    VCSpeaker.Files.games.path,
    GameData.serializer(),
    { Json.decodeFromString(this) },

    version = 1
) {
    private const val LAST_FETCHED_AT_ID = -1L

    suspend fun find(id: Long): GameData? = withData { data.find { it.id == id } }

    suspend fun findAll(ids: Set<Long>): Map<Long, String> = withData {
        data.filter { it.id in ids }.associate { it.id to it.name }
    }

    suspend fun lastFetchedAt(): Long? = withData { data.find { it.id == LAST_FETCHED_AT_ID }?.updatedAt }

    suspend fun replaceAll(games: Map<Long, String>, updatedAt: Long): Unit = withData {
        data = (games.map { (id, name) -> GameData(id, name, updatedAt) } + GameData(LAST_FETCHED_AT_ID, "", updatedAt))
            .toMutableList()
        writeLocked()
    }
}
