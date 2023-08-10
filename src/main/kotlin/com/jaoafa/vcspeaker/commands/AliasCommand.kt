package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Alias
import com.jaoafa.vcspeaker.features.Alias.Companion.fieldAliasFrom
import com.jaoafa.vcspeaker.store.AliasData
import com.jaoafa.vcspeaker.store.AliasStore
import com.jaoafa.vcspeaker.store.AliasType
import com.jaoafa.vcspeaker.tools.*
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand

class AliasCommand : Extension() {
    override val name = this::class.simpleName!!

    inner class CreateOptions : Arguments() {
        val type by stringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.values())
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

    inner class UpdateOptions : Arguments() {
        val search by string {
            name = "alias"
            description = "更新するエイリアス"

            autoComplete(Alias.autocomplete)
        }

        val type by optionalStringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.values())
                choice(aliasType.displayName, aliasType.name)
        }

        val to by optionalString {
            name = "to"
            description = "置き換える文字列"
        }
    }

    inner class DeleteOptions : Arguments() {
        val search by string {
            name = "alias"
            description = "削除するエイリアス"

            autoComplete(Alias.autocomplete)
        }
    }

    // todo AliasType diff
    override suspend fun setup() {
        publicSlashCommand {
            name = "alias"
            description = "エイリアスを設定します。"

            devGuild()

            publicSubCommand(::CreateOptions) {
                name = "create"
                description = "エイリアスを作成します。"

                action {
                    val type = AliasType.valueOf(arguments.type)
                    val from = arguments.from
                    val to = arguments.to

                    val duplicate = AliasStore.find(guild!!.id, from)
                    val oldTo = duplicate?.to

                    if (duplicate != null) AliasStore.remove(duplicate)

                    AliasStore.create(AliasData(guild!!.id, user.id, type, from, to))

                    respondEmbed(
                        ":loudspeaker: ${type.displayName}のエイリアスを${if (duplicate != null) "更新" else "作成"}しました"
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

            publicSubCommand(::UpdateOptions) {
                name = "update"
                description = "エイリアスを更新します。"

                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.search)
                    if (aliasData != null) {
                        val (_, _, type, from, to) = aliasData

                        val updatedType = arguments.type?.let { typeString -> AliasType.valueOf(typeString) } ?: type
                        val updatedTo = arguments.to ?: to

                        AliasStore.remove(aliasData)
                        AliasStore.create(aliasData.copy(userId = user.id, type = updatedType, to = updatedTo))

                        respondEmbed(":repeat: エイリアスを更新しました") {
                            authorOf(user)

                            fieldAliasFrom(updatedType, from)

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                "$to → **${updatedTo}**"
                            }

                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: エイリアスが見つかりません",
                            "置き換え条件が「${arguments.search}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }
                }
            }

            publicSubCommand(::DeleteOptions) {
                name = "delete"
                description = "エイリアスを削除します。"

                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.search)

                    if (aliasData != null) {
                        AliasStore.remove(aliasData)

                        val (_, _, type, from, to) = aliasData

                        respondEmbed(":wastebasket: エイリアスを削除しました") {
                            authorOf(user)

                            fieldAliasFrom(type, from)

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                to
                            }

                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: エイリアスが見つかりません",
                            "置き換え条件が「${arguments.search}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }
                }
            }

            publicSubCommand {
                name = "list"
                description = "エイリアスの一覧を表示します。"

                action {

                }
            }
        }
    }
}