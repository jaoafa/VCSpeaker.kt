package com.jaoafa.vcspeaker.tts.providers.soundboard

import com.jaoafa.vcspeaker.tools.hashMd5
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.SpeechProvider
import dev.kord.common.entity.Snowflake
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*

data class SoundboardContext(
    val id: Snowflake
) : ProviderContext {
    override fun describe() = "Soundboard: $id"

    override fun hash() = hashMd5(SoundboardProvider.id + id.toString())
}

object SoundboardProvider : SpeechProvider<SoundboardContext> {
    override val id = "soundboard"
    override val format = "mp3"

    private const val BASE_URL = "https://cdn.discordapp.com/soundboard-sounds/"
    private val client = HttpClient(CIO)

    override suspend fun provide(context: SoundboardContext): ByteArray {
        val response = client.get(BASE_URL + context.id)

        return when (response.status) {
            HttpStatusCode.OK -> response.body<ByteArray>()

            else -> throw Exception("Failed to fetch soundboard $context: ${response.status}")
        }
    }
}