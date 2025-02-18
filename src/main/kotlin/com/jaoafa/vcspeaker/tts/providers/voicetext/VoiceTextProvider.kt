package com.jaoafa.vcspeaker.tts.providers.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.tools.hashMd5
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.SpeechProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider.id
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * VoiceText で音声を生成する際のパラメーターを保持するクラスです。
 *
 * @param voice 音声
 * @param text テキスト
 */
data class VoiceTextContext(
    val voice: Voice,
    val text: String
) : ProviderContext {
    override fun describe() = "VoiceText: $text"

    override fun hash() = hashMd5(id + text + voice.toJson())
}

/**
 * VoiceText による音声合成を提供するクラスです。
 */
object VoiceTextProvider : SpeechProvider<VoiceTextContext> {
    override val id = "voicetext"
    override val format = "wav"

    private const val BASE_URL = "https://api.voicetext.jp/v1/tts"
    private val client = HttpClient(CIO)

    override suspend fun provide(context: VoiceTextContext): ByteArray {
        return generateSpeech(context.text, context.voice)
    }

    private suspend fun generateSpeech(
        text: String,
        voice: Voice
    ): ByteArray {
        val response = client.post(BASE_URL) {
            parameter("text", text)
            Json.parseToJsonElement(voice.toJson()).jsonObject.toMap().forEach { (t, u) ->
                parameter(t, u.jsonPrimitive.content.lowercase())
            }
            basicAuth(VCSpeaker.config[TokenSpec.voicetext], "")
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