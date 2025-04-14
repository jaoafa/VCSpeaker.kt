package com.jaoafa.vcspeaker.tts.providers.soundmoji

import com.jaoafa.vcspeaker.tools.VCSpeakerUserAgent
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.SpeechProvider
import dev.kord.common.entity.Snowflake
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

/**
 * Soundmoji を再生する際のパラメーターを保持するクラスです。
 *
 * @param id Soundmoji の ID
 */
@Serializable
data class SoundmojiContext(
    val id: Snowflake
) : ProviderContext {
    override fun describe() = "Soundmoji: $id"

    override fun identity() = SoundmojiProvider.id + id.toString()
}

/**
 * Discord Soundmoji の再生機能を提供するクラスです。
 */
object SoundmojiProvider : SpeechProvider<SoundmojiContext> {
    override val id = "soundmoji"
    override val format = "mp3"

    private const val BASE_URL = "https://cdn.discordapp.com/soundboard-sounds/"
    private val client = HttpClient(CIO) {
        VCSpeakerUserAgent()
    }

    override suspend fun provide(context: SoundmojiContext): ByteArray {
        val response = client.get(BASE_URL + context.id)

        return when (response.status) {
            HttpStatusCode.OK -> response.body<ByteArray>()

            else -> throw IOException("Failed to fetch soundboard $context: ${response.status}")
        }
    }
}