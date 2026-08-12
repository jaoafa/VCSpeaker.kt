package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.models.response.discord.DiscordDetectableApplication
import com.jaoafa.vcspeaker.models.response.discord.DiscordRateLimitResponse
import com.jaoafa.vcspeaker.tools.VCSpeakerUserAgent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Discord の非公開 API `applications/detectable` との通信を担当する。
 * このエンドポイントは認証不要のため、Authorization ヘッダーは付与しない。
 */
object DiscordGameApi {
    private val logger = KotlinLogging.logger {}

    private const val URL = "https://discord.com/api/v10/applications/detectable"

    private val client = HttpClient(CIO) {
        VCSpeakerUserAgent()

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }
    }

    // 429 応答時のプロセスローカル cooldown。プロセス内メモリのみで永続化せず、再起動でリセットされる。
    private var cooldownUntil: Long? = null

    /**
     * ゲーム一覧を全件取得する。取得に失敗した場合は例外を伝播させず null を返し、
     * TTS 全体を停止させない。
     */
    suspend fun getDetectableGames(): Map<Long, String>? {
        val cooldown = cooldownUntil
        if (cooldown != null && System.currentTimeMillis() < cooldown) {
            logger.info { "Skipping applications/detectable fetch due to active cooldown." }
            return null
        }

        return try {
            val response = client.get(URL)

            if (response.status == HttpStatusCode.TooManyRequests) {
                val retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toDoubleOrNull()
                    ?: try {
                        response.body<DiscordRateLimitResponse>().retryAfter
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to decode rate limit response body for applications/detectable." }
                        null
                    }
                logger.warn { "Rate limited on applications/detectable. retryAfterSeconds=$retryAfterSeconds" }
                applyCooldown(retryAfterSeconds)
                return null
            }

            if (response.status.value !in 200..299) {
                logger.warn { "Failed to fetch applications/detectable. status=${response.status}" }
                return null
            }

            response.body<List<DiscordDetectableApplication>>()
                .mapNotNull { app -> app.id.toLongOrNull()?.let { it to app.name } }
                .toMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch applications/detectable." }
            null
        }
    }

    private fun applyCooldown(retryAfterSeconds: Double?) {
        val seconds = retryAfterSeconds ?: return
        cooldownUntil = System.currentTimeMillis() + (seconds * 1000).toLong()
    }
}
