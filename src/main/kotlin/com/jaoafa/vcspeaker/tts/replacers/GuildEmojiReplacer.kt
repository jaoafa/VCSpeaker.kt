package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * Guildの絵文字を置換するクラス
 */
object GuildEmojiReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) = buildList {
        val regex = Regex("<a?:(\\w+):(\\d+)>")
        for (token in tokens) {
            val text = token.text

            if (token.replaced || !text.partialMatch(regex)) {
                add(token)
                continue
            }

            val matches = regex.findAll(text).toList()

            val splitTexts = text.split(regex)

            val additions = splitTexts.mixin { index ->
                val match = matches[index]
                val emojiName = match.groupValues[1]

                Token(emojiName, true)
            }

            addAll(additions)
        }
    }.toMutableList()
}