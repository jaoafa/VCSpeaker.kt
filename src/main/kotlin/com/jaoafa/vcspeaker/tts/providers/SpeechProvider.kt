package com.jaoafa.vcspeaker.tts.providers

import com.jaoafa.vcspeaker.tts.providers.soundboard.SoundboardContext
import com.jaoafa.vcspeaker.tts.providers.soundboard.SoundboardProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider

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
        is SoundboardContext -> SoundboardProvider as SpeechProvider<T>
        is VoiceTextContext -> VoiceTextProvider as SpeechProvider<T>
        else -> null
    }
}

fun getProvider(id: String): SpeechProvider<*>? {
    return when (id) {
        SoundboardProvider.id -> SoundboardProvider
        VoiceTextProvider.id -> VoiceTextProvider
        else -> null
    }
}