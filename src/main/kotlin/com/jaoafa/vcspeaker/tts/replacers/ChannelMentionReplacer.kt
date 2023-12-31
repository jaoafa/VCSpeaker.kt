package com.jaoafa.vcspeaker.tts.replacers

import dev.kord.common.entity.Snowflake

/**
 * チャンネルメンションを置換するクラス
 */
object ChannelMentionReplacer : BaseReplacer {
    override val priority = ReplacerPriority.High

    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<#(\\d+)>")) { kord, id ->
            kord.getChannel(id)?.data?.name?.value ?: "不明なチャンネル"
        }
}