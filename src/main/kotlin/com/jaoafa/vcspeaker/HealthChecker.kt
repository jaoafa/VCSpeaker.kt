package com.jaoafa.vcspeaker

import dev.kord.core.Kord
import dev.schlaubi.lavakord.LavaKord
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lavalink ノードのヘルスチェックを行い、必要に応じて自動再接続するオブジェクトです。
 */
object HealthChecker {
    private val logger = KotlinLogging.logger {}

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val checkIntervalMillis = 60_000L // 1 分

    /**
     * ヘルスチェックを開始します。
     *
     * @param kord Kord インスタンス
     * @param uri Lavalink サーバーの URI
     * @param password Lavalink サーバーのパスワード
     */
    fun start(kord: Kord, uri: String, password: String) {
        // 既に起動している場合は停止
        stop()

        job = scope.launch {
            logger.info { "Lavalink ヘルスチェックを開始しました" }

            while (isActive) {
                try {
                    val lavalink = VCSpeaker.lavalink
                    val nodesAvailable = lavalink.nodes.any { it.available }

                    if (!nodesAvailable) {
                        logger.warn { "Lavalink ノードが利用できません。再接続を試行します..." }

                        try {
                            // Lavalink への再接続を試行
                            initLavaLink(kord, uri, password)
                            logger.info { "Lavalink への再接続に成功しました" }
                        } catch (e: Exception) {
                            logger.error(e) { "Lavalink への再接続に失敗しました: ${e.message}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Lavalink ヘルスチェック中にエラーが発生しました: ${e.message}" }
                }

                // 次のチェックまで待機
                delay(checkIntervalMillis)
            }
        }
    }

    /**
     * ヘルスチェックを停止します。
     */
    fun stop() {
        job?.cancel()
        job = null
        logger.info { "Lavalink ヘルスチェックを停止しました" }
    }
}
