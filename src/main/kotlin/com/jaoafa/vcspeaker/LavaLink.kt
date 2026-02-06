package com.jaoafa.vcspeaker

import dev.kord.core.Kord
import dev.schlaubi.lavakord.kord.lavakord
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Lavalink への接続を初期化します。指数バックオフでリトライします。
 *
 * @param kord Kord インスタンス
 * @param uri Lavalink サーバーの URI
 * @param password Lavalink サーバーのパスワード
 * @throws IllegalStateException 最大リトライ回数を超えて接続に失敗した場合
 */
suspend fun initLavaLink(kord: Kord, uri: String, password: String) {
    val maxRetries = 5
    val baseDelayMillis = 1000L
    val jitterMillis = 500L

    repeat(maxRetries) { attempt ->
        try {
            val lavalink = kord.lavakord()
            lavalink.addNode(uri, password)

            // ノードが利用可能になるまで少し待機
            delay(500)

            // ノードの可用性を確認
            if (lavalink.nodes.any { it.available }) {
                VCSpeaker.lavalink = lavalink
                logger.info { "Lavalink 接続に成功しました (試行回数: ${attempt + 1})" }
                return
            } else {
                logger.warn { "Lavalink ノードが利用できません (試行回数: ${attempt + 1})" }
            }
        } catch (e: Exception) {
            logger.warn { "Lavalink 接続に失敗しました (試行回数: ${attempt + 1}): ${e.message}" }
        }

        // 最後のリトライでない場合は待機
        if (attempt < maxRetries - 1) {
            val delayTime = baseDelayMillis * (1 shl attempt) + Random.nextLong(-jitterMillis, jitterMillis)
            logger.info { "Lavalink 再接続まで ${delayTime}ms 待機します..." }
            delay(delayTime)
        }
    }

    // すべてのリトライが失敗した場合
    logger.error { "Lavalink 接続に失敗しました。最大リトライ回数 ($maxRetries) を超えました。" }
    throw IllegalStateException("Lavalink 接続に失敗しました。最大リトライ回数を超えました。")
}