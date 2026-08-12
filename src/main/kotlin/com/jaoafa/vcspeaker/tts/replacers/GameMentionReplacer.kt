package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.tools.discord.GameResolver
import com.jaoafa.vcspeaker.tts.TextToken
import dev.kord.common.entity.Snowflake

/**
 * ゲームメンションを置換するクラス
 */
object GameMentionReplacer : BaseReplacer {
    override val priority = ReplacerPriority.Normal

    private val regex = Regex("<@\\$(\\d+)>")

    override suspend fun replace(tokens: MutableList<TextToken>, guildId: Snowflake): MutableList<TextToken> {
        val ids = tokens.flatMap { token ->
            regex.findAll(token.text).map { it.groupValues[1].toLong() }
        }.distinct()

        if (ids.isEmpty()) return tokens

        val resolved = GameResolver.resolve(ids)

        return buildList {
            for (token in tokens) {
                val text = token.text

                if (token.replaced() || !text.partialMatch(regex)) {
                    add(token)
                    continue
                }

                val matches = regex.findAll(text).toList()
                val splitTexts = text.split(regex)

                val additions = splitTexts.mixin { index ->
                    val match = matches[index]
                    val id = match.groupValues[1].toLong()
                    val name = resolved[id]

                    if (name != null) {
                        TextToken(name, "Game `$id` →「$name」")
                    } else {
                        TextToken("ゲーム", "Game `$id` → 不明")
                    }
                }

                addAll(additions.filter { it.text.isNotEmpty() })
            }
        }.toMutableList()
    }
}
