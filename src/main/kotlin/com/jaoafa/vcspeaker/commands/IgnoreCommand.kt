package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.IgnoreData
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respondingPaginator
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringCollection

class IgnoreCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class CreateOptions : Options() {
        val text by string {
            name = "text"
            description = "無視する文字列"
        }
    }

    inner class DeleteOptions : Options() {
        val text by string {
            name = "text"
            description = "無視する文字列"

            autoComplete { event ->
                val guildId = event.interaction.getChannel().data.guildId.value

                suggestStringCollection(
                    IgnoreStore.filter(guildId).map { it.text },
                    FilterStrategy.Contains
                )
            }
        }
    }

    override suspend fun setup() {
        publicSlashCommand("ignore", "無視機能を設定します。") {
            publicSubCommand("create", "無視する文字列を作成します。", ::CreateOptions) {
                action {
                    val text = arguments.text
                    val duplicateExists = IgnoreStore.find(guild!!.id, text) != null

                    if (!duplicateExists)
                        IgnoreStore.create(IgnoreData(guild!!.id, user.id, text))

                    respondEmbed(
                        ":face_with_symbols_over_mouth: Ignore Created",
                        "今後「$text」を含むメッセージは読み上げられません。"
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }

            publicSubCommand("delete", "無視する文字列を削除します。", ::DeleteOptions) {
                action {
                    val text = arguments.text
                    val target = IgnoreStore.find(guild!!.id, text)

                    if (target != null) {
                        IgnoreStore.remove(target)

                        respondEmbed(
                            ":wastebasket: Ignore Deleted",
                            "「$text」が含まれていても読み上げます。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: Ignore Not Found",
                            "「$text」を含むメッセージは無視されていません。"
                        )
                    }
                }
            }

            publicSubCommand("list", "無視する文字列の一覧を表示します。") {
                action {
                    val ignores = IgnoreStore.filter(guild!!.id)

                    if (ignores.isEmpty()) {
                        respondEmbed(
                            ":grey_question: Ignores Not Found",
                            "無視機能が設定されていないようです。`/ignore create` で作成してみましょう！"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    respondingPaginator {
                        for (chunkedIgnores in ignores.chunked(10)) {
                            page {
                                authorOf(user)

                                description = chunkedIgnores.joinToString("\n") {
                                    "「${it.text}」<@${it.userId}>"
                                }

                                successColor()
                            }
                        }
                    }.send()
                }
            }
        }
    }
}