package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.stores.GameStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * game ID からゲーム名を解決する。キャッシュ(GameStore)の fresh/stale 判定は
 * game ID 単位ではなく、カタログ全体を最後に取得した時刻(GameStore.lastFetchedAt())
 * を基準に行う。
 */
object GameResolver {
    private val ttlMillis = TimeUnit.DAYS.toMillis(90)

    // カタログの再取得を直列化する。キーは単一(カタログ全体で 1 つ)のため Mutex のみで足りる。
    private val refreshMutex = Mutex()

    suspend fun resolve(gameIds: List<Long>): Map<Long, String?> {
        refreshCatalogIfStale()

        return gameIds.associateWith { id -> GameStore.find(id)?.name }
    }

    private suspend fun refreshCatalogIfStale() {
        if (!isStale()) return

        refreshMutex.withLock {
            // ロック取得までの間に他の呼び出しが再取得済みの可能性があるため再チェックする
            if (!isStale()) return@withLock

            val games = DiscordGameApi.getDetectableGames() ?: return@withLock
            GameStore.replaceAll(games, System.currentTimeMillis())
        }
    }

    private suspend fun isStale(): Boolean {
        val lastFetchedAt = GameStore.lastFetchedAt() ?: return true
        return System.currentTimeMillis() - lastFetchedAt > ttlMillis
    }
}
