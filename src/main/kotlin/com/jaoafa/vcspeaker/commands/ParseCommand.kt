package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tools.Emoji.containsEmojis
import com.jaoafa.vcspeaker.tools.Emoji.getEmojis
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tts.Token
import com.jaoafa.vcspeaker.tts.processors.ReplacerProcessor
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension

class ParseCommand : Extension() {
    override val name = this::class.simpleName!!

    inner class ParseOptions : Options() {
        val text by string {
            name = "message"
            description = "試すメッセージ"
        }
    }

    override suspend fun setup() {
        publicSlashCommand("parse", "読み上げる文章の処理をテストします", ::ParseOptions) {
            check { anyGuild() }
            action {
                val guildId = guild!!.id
                val text = arguments.text

                fun effectiveIgnores(text: String) = IgnoreStore.filter(guildId).filter {
                    when (it.type) {
                        IgnoreType.Equals -> text == it.search
                        IgnoreType.Contains -> text.contains(it.search)
                    }
                }

                val effectiveIgnores = effectiveIgnores(text)

                suspend fun respondStepEmbed(
                    checkIgnore: String? = null,
                    applyAlias: String? = null,
                    recheckIgnore: String? = null,
                    replaceEmoji: String? = null,
                    result: String,
                    ignored: Boolean
                ) = respondEmbed(
                    ":alembic: Text Parsed"
                ) {
                    field(":a: 入力") {
                        "「$text」"
                    }

                    fun stepField(step: Int, title: String, text: String?) {
                        val emojis = listOf(":one:", ":two:", ":three:", ":four:")
                        val emoji = if (text != null) emojis.getOrNull(step - 1) else ":white_large_square:"

                        field("$emoji $title") {
                            text ?: "＊スキップされました。"
                        }
                    }

                    stepField(
                        step = 1,
                        title = "無視するか確認",
                        text = checkIgnore
                    )

                    stepField(
                        step = 2,
                        title = "エイリアスを適用",
                        text = applyAlias
                    )

                    stepField(
                        step = 3,
                        title = "無視するか再確認",
                        text = recheckIgnore
                    )

                    stepField(
                        step = 4,
                        title = "Unicode 絵文字を置き換え",
                        text = replaceEmoji
                    )

                    field(":white_check_mark: 結果") {
                        result
                    }

                    if (ignored) errorColor() else successColor()
                }

                // step 1: check ignore
                if (effectiveIgnores.isNotEmpty()) {
                    respondStepEmbed(
                        checkIgnore = effectiveIgnores.joinToString("\n") {
                            it.toDisplay()
                        },
                        result = "＊無視されました。",
                        ignored = true
                    )

                    return@action
                }

                // step 2: apply alias
                val tokens = ReplacerProcessor().replacers.fold(mutableListOf(Token(text))) { tokens, replacer ->
                    replacer.replace(tokens, guildId)
                }

                // annotate text with alias index
                var aliasIndex = 1
                val annotatedText = tokens.joinToString("") {
                    it.text + if (it.replaced()) {
                        val annotation = " `[$aliasIndex]` "
                        aliasIndex++
                        annotation
                    } else ""
                }

                // step 3: recheck ignore
                val annotatedEffectiveIgnores = effectiveIgnores(annotatedText)

                val replacedTokens = tokens.filter { it.replaced() }

                val replaceResult = "「$annotatedText」\n" + replacedTokens.withIndex()
                    .joinToString("\n") { (i, token) -> "$i. ${token.replacer}" }

                if (annotatedEffectiveIgnores.isNotEmpty()) {
                    respondStepEmbed(
                        checkIgnore = "＊無視されませんでした。",
                        applyAlias = replaceResult,
                        recheckIgnore = annotatedEffectiveIgnores.joinToString("\n") {
                            it.toDisplay()
                        },
                        result = "＊無視されました。",
                        ignored = true
                    )

                    return@action
                }

                // step 4: replace emoji
                val appliedText = tokens.joinToString("") { it.text }
                val emojis = appliedText.getEmojis()

                var emojiIndex = 1
                val (annotatedAppliedText, result) = emojis.fold(appliedText to appliedText) { (annotated, result), emoji ->
                    val newAnnotated = annotated.replace(emoji.unicode, "${emoji.name} `[$emojiIndex]`")
                    val newResult = result.replace(emoji.unicode, emoji.name)

                    emojiIndex++

                    newAnnotated to newResult
                }

                val emojiReplaceResult = "「$annotatedAppliedText」\n" + emojis
                    .withIndex().joinToString("\n") { (i, emoji) -> "$i. ${emoji.unicode} → ${emoji.name}" }

                respondStepEmbed(
                    checkIgnore = "＊無視されませんでした。",
                    applyAlias = if (replacedTokens.isNotEmpty()) replaceResult else "＊置き換えられませんでした。",
                    recheckIgnore = "＊無視されませんでした。",
                    replaceEmoji = if (appliedText.containsEmojis()) emojiReplaceResult else "＊絵文字は含まれていませんでした。",
                    result = "「$result」",
                    ignored = false
                )
            }
        }
    }
}