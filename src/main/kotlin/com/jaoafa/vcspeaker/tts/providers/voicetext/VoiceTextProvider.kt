package com.jaoafa.vcspeaker.tts.providers.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.configs.TokenSpec
import com.jaoafa.vcspeaker.tools.VCSpeakerUserAgent
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.SpeechProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider.id
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * VoiceText で音声を生成する際のパラメーターを保持するクラスです。
 *
 * @param voice 音声
 * @param text テキスト
 */
@Serializable
@SerialName("voicetext")
data class VoiceTextContext(
    val voice: Voice,
    val text: String
) : ProviderContext {
    override fun describe() = "VoiceText: $text"

    override fun identity() = id + text + voice.toJson()
}

/**
 * VoiceText による音声合成を提供するクラスです。
 */
object VoiceTextProvider : SpeechProvider<VoiceTextContext> {
    override val id = "voicetext"
    override val format = "wav"

    private val client = HttpClient(CIO) {
        install(Resources)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        VCSpeakerUserAgent()
        defaultRequest {
            host = "api.voicetext.jp"
            url {
                protocol = URLProtocol.HTTPS
            }
        }
    }

    override suspend fun provide(context: VoiceTextContext): ByteArray {
        return generateSpeech(context)
    }

    private suspend fun generateSpeech(context: VoiceTextContext): ByteArray {
        val response = client.post(VoiceTextResource.fromContext(context)) {
            basicAuth(VCSpeaker.config[TokenSpec.voicetext], "")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<ByteArray>()

            else -> throw IOException(
                Json.decodeFromString(
                    VoiceTextError.serializer(),
                    response.bodyAsText()
                ).error.message
            )
        }
    }
}