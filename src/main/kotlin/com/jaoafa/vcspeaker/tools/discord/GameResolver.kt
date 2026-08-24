package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.database.actions.GameAction
import com.jaoafa.vcspeaker.database.tables.GameEntity
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toLong
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toSnowflake
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.TimeUnit

/**
 * game ID からゲーム名を解決する。fresh/stale 判定は game ID 単位ではなく、
 * カタログ全体の最終取得時刻(GameResolver.lastFetchedAt)を基準に行う。
 */
object GameResolver {
    var lastFetchedAt: Long? = null
        private set

    fun setFetchTimestamp(lastFetchedAt: Long) {
        this.lastFetchedAt = lastFetchedAt
    }

    private val ttlMillis = TimeUnit.DAYS.toMillis(7)

    // カタログの再取得を直列化する。キーは単一(カタログ全体で 1 つ)のため Mutex のみで足りる。
    private val refreshMutex = Mutex()

    // 取得失敗時のプロセスローカル cooldown。プロセス内メモリのみで永続化せず、再起動でリセットされる。
    // 固定値であり仕様値ではない。再取得の連続失敗によるリトライストームを避けるためのもの。
    private var refreshFailedUntil: Long? = null
    private val failureCooldownMillis = TimeUnit.MINUTES.toMillis(5)

    suspend fun resolve(gameIds: List<Long>): Map<Long, String?> {
        refreshCatalogIfStale()

        return transaction {
            GameEntity.forIds(gameIds.map { it.toSnowflake() }).associate { it.id.value.toLong() to it.name }
        }
    }

    private suspend fun refreshCatalogIfStale() {
        if (!isStale()) return

        refreshMutex.withLock {
            // ロック取得までの間に他の呼び出しが再取得済みの可能性があるため再チェックする
            if (!isStale()) return@withLock

            val failedUntil = refreshFailedUntil
            if (failedUntil != null && System.currentTimeMillis() < failedUntil) return@withLock

            val games = DiscordGameApi.getDetectableGames()
            if (games == null) {
                refreshFailedUntil = System.currentTimeMillis() + failureCooldownMillis
                return@withLock
            }
            GameAction.replaceAll(games)
        }
    }

    private fun isStale() = lastFetchedAt?.let { System.currentTimeMillis() - it > ttlMillis } ?: true
}
