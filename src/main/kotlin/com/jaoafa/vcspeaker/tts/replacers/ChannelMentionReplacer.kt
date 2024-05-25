package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * チャンネルメンションを置換するクラス
 */
object ChannelMentionReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceMentionable(tokens, Regex("<#(\\d+)>")) { kord, id ->
            kord.getChannel(id)?.data?.name?.value ?: "不明なチャンネル"
        }
}