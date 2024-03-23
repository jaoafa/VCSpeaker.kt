package com.jaoafa.vcspeaker.tools

import com.jaoafa.vcspeaker.models.response.steam.SteamAppDetail
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object Steam {
    private const val BASE_URL = "https://store.steampowered.com/api/appdetails"

    private val client = HttpClient(CIO) {
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

    suspend fun getAppDetail(appId: String): SteamAppDetail? {
        val response = client.get(BASE_URL) {
            parameter("appids", appId)
            parameter("cc", "JP")
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val mapSerializer = MapSerializer(String.serializer(), SteamAppDetail.serializer())
                val map = JsonConfiguration.decodeFromString(mapSerializer, response.body())
                map[appId] ?: return null
            }

            else -> null
        }
    }
}