package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.tts.Token
import dev.kord.common.entity.Snowflake

/**
 * ロールメンションを置換するクラス
 */
object RoleMentionReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake) =
        replaceMentionable(tokens, Regex("<@&(\\d+)>"), "@") { kord, id ->
            kord.getGuildOrNull(guildId)?.getRole(id)?.data?.name ?: "不明なロール"
        }
}