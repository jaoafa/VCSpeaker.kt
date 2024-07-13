package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Emotion
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import dev.kord.core.entity.Message

class InlineVoiceProcessor : BaseProcessor() {
    override val priority = 40

    private val syntax = Regex("(speaker|emotion|emotion_level|pitch|speed|volume):\\w+")

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val parameters = syntax.findAll(content).map { it.value }

        val parameterMap = parameters.map {
            val (key, value) = it.split(":")
            key to value
        }.toMap()

        val newVoice = voice.copyNotNull(
            speaker = parameterMap["speaker"]?.let { Speaker.valueOf(it.capitalizeWords()) },
            emotion = parameterMap["emotion"]?.let { Emotion.valueOf(it.capitalizeWords()) },
            emotionLevel = parameterMap["emotion_level"]?.toIntOrNull(),
            pitch = parameterMap["pitch"]?.toIntOrNull(),
            speed = parameterMap["speed"]?.toIntOrNull()
        )

        val newText = parameters.fold(content) { replacedText, parameterText ->
            replacedText.replace(parameterText, "")
        }.trim()

        return newText to newVoice
    }
}