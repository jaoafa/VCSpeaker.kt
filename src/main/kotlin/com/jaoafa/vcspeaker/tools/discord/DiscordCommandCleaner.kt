package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.tools.VCSpeakerUserAgent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object DiscordCommandCleaner {
    private val logger = KotlinLogging.logger {}

    @Serializable
    private data class ApplicationInfo(
        val id: String
    )

    suspend fun clearRegisteredCommands(token: String, devGuildId: Long?) {
        HttpClient(CIO) {
            VCSpeakerUserAgent()

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }

            expectSuccess = false

            defaultRequest {
                header(HttpHeaders.Authorization, "Bot $token")
                contentType(ContentType.Application.Json)
            }
        }.use { client ->
            val appId = try {
                val response = client.get("https://discord.com/api/v10/oauth2/applications/@me")
                if (response.status.value !in 200..299) {
                    logger.error { "Failed to fetch application info. status=${response.status}" }
                    return
                }
                val appInfo = response.body<ApplicationInfo>()
                appInfo.id
            } catch (e: Exception) {
                logger.error(e) { "Exception while fetching application info from Discord." }
                return
            }

            val globalResponse = client.put("https://discord.com/api/v10/applications/$appId/commands") {
                setBody("[]")
            }

            if (globalResponse.status.value !in 200..299) {
                logger.error { "Failed to delete global application commands. status=${globalResponse.status}" }
            } else {
                logger.info { "Deleted global application commands." }
            }

            if (devGuildId != null) {
                val guildResponse = client.put("https://discord.com/api/v10/applications/$appId/guilds/$devGuildId/commands") {
                    setBody("[]")
                }

                if (guildResponse.status.value !in 200..299) {
                    logger.error { "Failed to delete guild application commands for $devGuildId. status=${guildResponse.status}" }
                } else {
                    logger.info { "Deleted guild application commands for $devGuildId." }
                }
            } else {
                logger.info { "Dev guild is not set. Skipping guild command cleanup." }
            }
        }
    }
}
