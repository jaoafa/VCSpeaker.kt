package com.jaoafa.vcspeaker.voicetext.textreplacers

import dev.kord.common.entity.Snowflake

object GuildEmojiReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String {
        val matches = Regex("<a?:(\\w+):(\\d+)>").findAll(text)

        val replacedText = matches.fold(text) { replacedText, match ->
            val emojiName = match.groupValues[1]

            replacedText.replace(match.value, emojiName)
        }

        return replacedText
    }
}