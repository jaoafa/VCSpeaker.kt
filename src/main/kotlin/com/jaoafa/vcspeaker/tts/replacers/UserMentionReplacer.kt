package com.jaoafa.vcspeaker.tts.replacers

import dev.kord.common.entity.Snowflake

/**
 * ユーザーメンションを置換するクラス
 */
object UserMentionReplacer : BaseReplacer {
    override suspend fun replace(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@!?(\\d+)>")) { kord, id ->
            val effectiveName = kord.getGuildOrNull(guildId)?.getMember(id)?.effectiveName
            effectiveName ?: "不明なユーザー"
        }
}