package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.tools.Emoji.replaceEmojiToName
import com.jaoafa.vcspeaker.voicetext.api.Emotion
import com.jaoafa.vcspeaker.voicetext.api.Speaker
import com.jaoafa.vcspeaker.voicetext.textreplacers.*
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import dev.kord.common.entity.Snowflake

// ignore -> emoji -> regex -> text

object TextProcessor {

    suspend fun processText(guildId: Snowflake, text: String): String? {
        if (shouldIgnore(text, guildId)) return null

        val replacers = listOf(
            EmojiReplacer,
            GuildEmojiReplacer,
            RegexReplacer,
            AliasReplacer,
            ChannelMentionReplacer,
            RoleMentionReplacer,
            UserMentionReplacer,
            MessageMentionReplacer,
        )

        val replacedText = replacers.fold(text) { replacedText, replacer ->
            replacer.replace(replacedText, guildId)
        }.replaceEmojiToName()


        return replacedText.let { if (it.length > 180) it.substring(0, 180) else it }
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

    private fun shouldIgnore(text: String, guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            text.contains(it.text)
        }
}