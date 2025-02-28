package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.models.response.youtube.YouTubeOEmbedResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object YouTube {
    private const val BASE_URL = "https://www.youtube.com/oembed"

    private val client = HttpClient(CIO) {
        VCSpeakerUserAgent()

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val JsonConfiguration = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getVideo(videoId: String): YouTubeOEmbedResponse? {
        val response = client.get(BASE_URL) {
            parameter("url", "https://www.youtube.com/watch?v=$videoId")
            parameter("format", "json")
        }

        return when (response.status) {
            HttpStatusCode.OK -> JsonConfiguration.decodeFromString(response.body())
            else -> null
        }
    }

    suspend fun getPlaylist(playlistId: String): YouTubeOEmbedResponse? {
        val response = client.get(BASE_URL) {
            parameter("url", "https://www.youtube.com/playlist?list=$playlistId")
            parameter("format", "json")
        }

        return when (response.status) {
            HttpStatusCode.OK -> JsonConfiguration.decodeFromString(response.body())
            else -> null
        }
    }
}