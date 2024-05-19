package com.jaoafa.vcspeaker.tts.api

import com.jaoafa.vcspeaker.tts.Voice
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VoiceTextAPI(private val token: String) {
    private val baseURL = "https://api.voicetext.jp/v1/tts"
    private val client = HttpClient(CIO)

    suspend fun generateSpeech(
        text: String,
        voice: Voice
    ): ByteArray {
        val response = client.post(baseURL) {
            parameter("text", text)
            Json.parseToJsonElement(voice.toJson()).jsonObject.toMap().forEach { (t, u) ->
                parameter(t, u.jsonPrimitive.content.lowercase())
            }
            basicAuth(token, "")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<ByteArray>()

            else -> throw Exception(
                Json.decodeFromString(
                    VoiceTextError.serializer(),
                    response.bodyAsText()
                ).error.message
            )
        }
    }
}