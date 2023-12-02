package com.jaoafa.vcspeaker.tts.replacers

import dev.kord.common.entity.Snowflake

/**
 * Guildの絵文字を置換するクラス
 */
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