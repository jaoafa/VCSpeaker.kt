package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tools.Emoji.replaceEmojiToName
import com.jaoafa.vcspeaker.tools.getObjectsIn
import com.jaoafa.vcspeaker.tts.api.Emotion
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.markdown.toMarkdown
import com.jaoafa.vcspeaker.tts.replacers.BaseReplacer
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import dev.kord.common.entity.Snowflake

object TextProcessor {
    val replacers = getObjectsIn<BaseReplacer>("com.jaoafa.vcspeaker.tts.replacers")
        .filterNotNull()
        .sortedByDescending { it.priority.level }

    private fun String.shouldIgnoreOn(guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            when (it.type) {
                IgnoreType.Exact -> this == it.text
                IgnoreType.Contains -> contains(it.text)
            }
        }

    suspend fun processText(guildId: Snowflake, text: String): String? {
        if (text.shouldIgnoreOn(guildId)) return null

        val replacedText = replacers.fold(text) { replacedText, replacer ->
            replacer.replace(replacedText, guildId)
        }.replaceEmojiToName()

        val markdown = replacedText.toMarkdown().joinToString("") { it.toReadable() }

        return markdown.let { if (it.length > 180) it.substring(0, 180) else it }
    }

    fun extractInlineVoice(text: String, voice: Voice): Pair<String, Voice> {
        val syntax = Regex("(speaker|emotion|emotion_level|pitch|speed|volume):\\w+")

        val parameters = syntax.findAll(text).map { it.value }

        val parameterMap = parameters.map {
            val (key, value) = it.split(":")
            key to value
        }.toMap()

        val newVoice = voice.overwrite(
            speaker = parameterMap["speaker"]?.let { Speaker.valueOf(it.capitalizeWords()) },
            emotion = parameterMap["emotion"]?.let { Emotion.valueOf(it.capitalizeWords()) },
            emotionLevel = parameterMap["emotion_level"]?.toIntOrNull(),
            pitch = parameterMap["pitch"]?.toIntOrNull(),
            speed = parameterMap["speed"]?.toIntOrNull()
        )

        val newText = parameters.fold(text) { replacedText, parameterText ->
            replacedText.replace(parameterText, "")
        }.trim()

        return newText to newVoice
    }
}