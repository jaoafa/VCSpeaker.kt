package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Alias
import com.jaoafa.vcspeaker.features.Alias.fieldAliasFrom
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.Options
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respondingPaginator

class AliasCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class CreateOptions : Options() {
        val type by stringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.entries)
                choice(aliasType.displayName, aliasType.name)
        }

        val from by string {
            name = "from"
            description = "置き換える条件"
        }

        val to by string {
            name = "to"
            description = "置き換える文字列"
        }
    }

    inner class UpdateOptions : Options() {
        val search by string {
            name = "alias"
            description = "更新するエイリアス"

            autoComplete(Alias.autocomplete)
        }

        val type by optionalStringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.entries)
                choice(aliasType.displayName, aliasType.name)
        }

        val to by optionalString {
            name = "to"
            description = "置き換える文字列"
        }
    }

    inner class DeleteOptions : Options() {
        val search by string {
            name = "alias"
            description = "削除するエイリアス"

            autoComplete(Alias.autocomplete)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("alias", "エイリアスを設定します。") {
            publicSubCommand("create", "エイリアスを作成します。", ::CreateOptions) {
                action {
                    val type = AliasType.valueOf(arguments.type)
                    val from = arguments.from
                    val to = arguments.to

                    val duplicate = AliasStore.find(guild!!.id, from)
                    val oldTo = duplicate?.to

                    if (duplicate != null) AliasStore.remove(duplicate)

                    AliasStore.create(AliasData(guild!!.id, user.id, type, from, to))

                    respondEmbed(
                        ":loudspeaker: Alias ${if (duplicate != null) "Updated" else "Created"}",
                        "${type.displayName}のエイリアスを${if (duplicate != null) "更新" else "作成"}しました"
                    ) {
                        authorOf(user)

                        fieldAliasFrom(type, from)

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            if (duplicate != null) "$oldTo → **$to**" else to
                        }

                        successColor()
                    }
                }
            }

            publicSubCommand("update", "エイリアスを更新します。", ::UpdateOptions) {
                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.search)
                    if (aliasData != null) {
                        val (_, _, type, from, to) = aliasData

                        val updatedType = arguments.type?.let { typeString -> AliasType.valueOf(typeString) } ?: type
                        val updatedTo = arguments.to ?: to

                        AliasStore.remove(aliasData)
                        AliasStore.create(aliasData.copy(userId = user.id, type = updatedType, to = updatedTo))

                        respondEmbed(
                            ":repeat: Alias Updated",
                            "${type.displayName}のエイリアスを更新しました。"
                        ) {
                            authorOf(user)

                            fieldAliasFrom(updatedType, from)

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                "$to → **${updatedTo}**"
                            }

                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "置き換え条件が「${arguments.search}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }
                }
            }

            publicSubCommand("delete", "エイリアスを削除します。", ::DeleteOptions) {
                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.search)

                    if (aliasData != null) {
                        AliasStore.remove(aliasData)

                        val (_, _, type, from, to) = aliasData

                        respondEmbed(
                            ":wastebasket: Alias Deleted",
                            "${type.displayName}のエイリアスを削除しました。"
                        ) {
                            authorOf(user)

                            fieldAliasFrom(type, from)

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                to
                            }

                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "置き換え条件が「${arguments.search}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }
                }
            }

            publicSubCommand("list", "エイリアスの一覧を表示します。") {
                action {
                    val aliases = AliasStore.filter(guild!!.id)

                    if (aliases.isEmpty()) {
                        respondEmbed(
                            ":grey_question: Aliases Not Found",
                            "エイリアスが設定されていないようです。`/alias create` で作成してみましょう！"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    respondingPaginator {
                        for (chunkedAliases in aliases.chunked(10)) {
                            page {
                                authorOf(user)

                                for (alias in chunkedAliases) {
                                    val (_, _, type, from, to) = alias

                                    field("${type.emoji} ${type.displayName}", false) {
                                        "${if (type == AliasType.Regex) "`$from`" else from} → $to"
                                    }
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