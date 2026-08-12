package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GameData(
    val id: Long,
    val name: String,
    val updatedAt: Long
)

/**
 * `applications/detectable` から取得したゲーム一覧のキャッシュ。
 * `updatedAt` は game ID ごとの値ではなく、カタログ全体を最後に再取得した時刻。
 * [replaceAll] のたびに全件が同じ `updatedAt` で上書きされる。
 */
object GameStore : StoreStruct<GameData>(
    VCSpeaker.Files.games.path,
    GameData.serializer(),
    { Json.decodeFromString(this) },

    version = 1
) {
    suspend fun find(id: Long): GameData? = withData { data.find { it.id == id } }

    suspend fun lastFetchedAt(): Long? = withData { data.maxOfOrNull { it.updatedAt } }

    suspend fun replaceAll(games: Map<Long, String>, updatedAt: Long): Unit = withData {
        data = games.map { (id, name) -> GameData(id, name, updatedAt) }.toMutableList()
        writeLocked()
    }
}
