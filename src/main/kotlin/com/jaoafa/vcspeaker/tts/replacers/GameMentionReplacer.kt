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
        if (tokens.none { !it.replaced() && it.text.partialMatch(regex) }) return tokens

        val ids = tokens.flatMap { token ->
            regex.findAll(token.text).mapNotNull { it.groupValues[1].toLongOrNull() }
        }.distinct()

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
                    val rawId = match.groupValues[1]
                    val id = rawId.toLongOrNull()
                    val name = id?.let { resolved[it] }

                    if (name != null) {
                        TextToken(name, "Game `$rawId` →「$name」")
                    } else {
                        TextToken("ゲーム", "Game `$rawId` → 不明")
                    }
                }

                addAll(additions.filter { it.text.isNotEmpty() })
            }
        }.toMutableList()
    }
}
