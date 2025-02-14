package com.jaoafa.vcspeaker.tts.providers

import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext

interface SpeechProvider<T : ProviderContext> {
    val id: String
    val format: String

    suspend fun provide(context: T): ByteArray
}

interface ProviderContext {
    fun describe(): String

    fun hash(): String
}

fun <T : ProviderContext> providerOf(context: T): SpeechProvider<T>? {
    return when (context) {
        is SoundmojiContext -> SoundmojiProvider as SpeechProvider<T>
        is VoiceTextContext -> VoiceTextProvider as SpeechProvider<T>
        else -> null
    }
}

fun getProvider(id: String): SpeechProvider<*>? {
    return when (id) {
        SoundmojiProvider.id -> SoundmojiProvider
        VoiceTextProvider.id -> VoiceTextProvider
        else -> null
    }
}