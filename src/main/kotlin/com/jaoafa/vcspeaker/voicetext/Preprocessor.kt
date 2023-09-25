package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.voicetext.api.Emotion
import com.jaoafa.vcspeaker.voicetext.api.Speaker
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel

// ignore -> emoji -> regex -> text

object Preprocessor {

    suspend fun processText(guildId: Snowflake, text: String): String? {
        if (shouldIgnore(text, guildId)) return null

        suspend fun replace(vararg replacers: suspend (String, Snowflake) -> String) =
            replacers.fold(text) { replacedText, replacer ->
                replacer(replacedText, guildId)
            }

        val replacedText = replace(
            ::replaceEmoji,
            ::replaceRegex,
            ::replaceAlias,
            ::replaceChannelMention,
            ::replaceRoleMention,
            ::replaceUserMention,
            ::replaceMessageMention
        )

        return replacedText.let { if (it.length > 180) it.substring(0, 180) else it }
    }

    fun extractInlineVoice(text: String, voice: Voice): Pair<String, Voice> {
        val syntax = Regex("(speaker|emotion|emotion_level|pitch|speed|volume):.+")

        val parameters = syntax.findAll(text).map { it.value }

        val parameterMap = parameters.map {
            val (key, value) = it.split(":")
            key to value
        }.toMap()

        val newVoice = voice.overwrite(
            speaker = parameterMap["speaker"]?.let { Speaker.valueOf(it) },
            emotion = parameterMap["emotion"]?.let { Emotion.valueOf(it) },
            emotionLevel = parameterMap["emotion_level"]?.toIntOrNull(),
            pitch = parameterMap["pitch"]?.toIntOrNull(),
            speed = parameterMap["speed"]?.toIntOrNull()
        )

        val newText = parameters.fold(text) { partText, parameterText ->
            partText.replace(parameterText, "")
        }.trim()

        return newText to newVoice
    }

    private fun shouldIgnore(text: String, guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            text.contains(it.text)
        }

    private fun replaceEmoji(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Emoji) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    private fun replaceRegex(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Regex) { alias, replacedText ->
            replacedText.replace(Regex(alias.from), alias.to)
        }

    private fun replaceAlias(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Text) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    private fun replaceText(
        text: String,
        guildId: Snowflake,
        type: AliasType,
        transform: (AliasData, String) -> String
    ): String {
        val aliases = AliasStore.filter(guildId).filter { it.type == type }

        val replacedText = aliases.fold(text) { replacedText, alias ->
            transform(alias, replacedText)
        }

        return replacedText
    }

    private suspend fun replaceChannelMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<#(\\d+)>")) { kord, id ->
            kord.getChannel(id)?.data?.name?.value ?: "不明なチャンネル"
        }

    private suspend fun replaceRoleMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@&(\\d+)>")) { kord, id ->
            kord.getGuildOrNull(guildId)?.getRole(id)?.data?.name ?: "不明なロール"
        }

    private suspend fun replaceUserMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@!?(\\d+)>")) { kord, id ->
            val displayName = kord.getGuildOrNull(guildId)?.getMember(id)?.displayName
            displayName ?: "不明なユーザー"
        }

    private suspend fun replaceMessageMention(text: String, guildId: Snowflake): String {
        val matches = Regex("https://(\\w+\\.)*discord.com/channels/(\\d+)/(\\d+)/(\\d+)").findAll(text)

        val replacedText = matches.fold(text) { replacedText, match ->
            val channelId = Snowflake(match.groupValues[3])
            val messageId = Snowflake(match.groupValues[4])

            val channel = VCSpeaker.instance.kordRef.getChannelOf<TextChannel>(channelId)
            val message = channel?.getMessageOrNull(messageId) ?: return@fold replacedText

            val read = "${message.author?.username ?: "システム"} が ${channel.name} で送信したメッセージへのリンク"

            replacedText.replace(match.value, read)
        }

        return replacedText
    }

    private suspend fun replaceMentionable(
        text: String,
        regex: Regex,
        nameSupplier: suspend (Kord, Snowflake) -> String
    ): String {
        val matches = regex.findAll(text)

        val replacedText = matches.fold(text) { replacedText, match ->
            val id = Snowflake(match.groupValues[1]) // 0 is for whole match
            val name = nameSupplier(VCSpeaker.instance.kordRef, id)

            replacedText.replace(match.value, name)
        }

        return replacedText
    }
}