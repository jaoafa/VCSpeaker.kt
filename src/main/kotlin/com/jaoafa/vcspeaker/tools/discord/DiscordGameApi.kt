package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.models.response.discord.DiscordDetectableApplication
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Discord の非公開 API `applications/detectable` との通信を担当する。
 * このエンドポイントは認証不要であることを実際の Bot token で確認済みのため、
 * Authorization ヘッダーは付与しない。
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
     * ゲーム一覧を全件取得する。失敗時(HTTP 429/5xx/401/403、タイムアウト、
     * 接続失敗、JSON デコード失敗のいずれか)は例外を伝播させず null を返す。
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
                applyCooldown(response.headers[HttpHeaders.RetryAfter])
                return null
            }

            if (response.status.value !in 200..299) {
                logger.warn { "Failed to fetch applications/detectable. status=${response.status}" }
                return null
            }

            response.body<List<DiscordDetectableApplication>>()
                .mapNotNull { app -> app.id.toLongOrNull()?.let { it to app.name } }
                .toMap()
        } catch (e: IOException) {
            logger.warn(e) { "I/O error while fetching applications/detectable." }
            null
        } catch (e: SerializationException) {
            logger.warn(e) { "Failed to decode applications/detectable response." }
            null
        }
    }

    private fun applyCooldown(retryAfterHeader: String?) {
        val retryAfterSeconds = retryAfterHeader?.toDoubleOrNull() ?: return
        cooldownUntil = System.currentTimeMillis() + (retryAfterSeconds * 1000).toLong()
    }
}
