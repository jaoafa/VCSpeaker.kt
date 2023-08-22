package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.store.IgnoreData
import com.jaoafa.vcspeaker.store.IgnoreStore
import com.jaoafa.vcspeaker.tools.*
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
        publicSlashCommand("ignore", "無視項目を設定します。") {
            publicSubCommand("create", "無視する文字列を作成します。", ::CreateOptions) {
                action {
                    val text = arguments.text
                    val duplicateExists = IgnoreStore.find(guild!!.id, text) != null

                    if (!duplicateExists)
                        IgnoreStore.create(IgnoreData(guild!!.id, user.id, text))

                    respondEmbed(
                        ":loudspeaker: 無視する文字列を作成しました",
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
                            ":wastebasket: 無視する文字列を削除しました",
                            "「$text」が含まれていても読み上げます。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: 無視する文字列が見つかりません",
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
                            ":question: 無視する文字列が存在しません",
                            "`/ignore create` で無視する文字列を作成してみましょう！"
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