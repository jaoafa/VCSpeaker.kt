package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.TextChannel

object MessageMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String {
        val matches = Regex("https://(\\w+\\.)*discord.com/channels/(\\d+)/(\\d+)/(\\d+)").findAll(text)

        val replacedText = matches.fold(text) { replacedText, match ->
            val channelId = Snowflake(match.groupValues[3])
            val messageId = Snowflake(match.groupValues[4])

            val channel = VCSpeaker.kord.getChannelOf<TextChannel>(channelId)
            val message = channel?.getMessageOrNull(messageId) ?: return@fold replacedText

            val read = "${message.author?.username ?: "システム"} が ${channel.name} で送信したメッセージへのリンク"

            replacedText.replace(match.value, read)
        }

        return replacedText
    }
}