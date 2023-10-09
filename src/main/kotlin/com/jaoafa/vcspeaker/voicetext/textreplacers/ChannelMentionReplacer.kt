package com.jaoafa.vcspeaker.voicetext.textreplacers

import dev.kord.common.entity.Snowflake

/**
 * チャンネルメンションを置換するクラス
 */
object ChannelMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake): String =
        replaceMentionable(text, Regex("<#(\\d+)>")) { kord, id ->
            kord.getChannel(id)?.data?.name?.value ?: "不明なチャンネル"
        }
}