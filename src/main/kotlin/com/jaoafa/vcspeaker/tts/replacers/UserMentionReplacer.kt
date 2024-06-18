package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * ユーザーメンションを置換するクラス
 */
object UserMentionReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceMentionable(tokens, Regex("<@!?(\\d+)>"), "@") { kord, id ->
            val effectiveName = kord.getGuildOrNull(guildId)?.getMember(id)?.effectiveName
            effectiveName ?: "不明なユーザー"
        }
}