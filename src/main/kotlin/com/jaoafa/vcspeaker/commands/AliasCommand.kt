package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.store.AliasData
import com.jaoafa.vcspeaker.store.AliasStore
import com.jaoafa.vcspeaker.store.AliasType
import com.jaoafa.vcspeaker.tools.devGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.Color
import dev.kord.rest.builder.message.create.embed

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
            name = "search"
            description = "検索する文字列"

            autoComplete { event ->
                val guildId = event.interaction.getChannel().data.guildId.value ?: return@autoComplete
                val guild = kord.getGuildOrNull(guildId) ?: return@autoComplete

                suggestStringMap(
                    AliasStore.data.filter { it.guildId == guildId }.associate { "${it.type.displayName} / ${it.from} → ${it.to}" to it.from },
                    FilterStrategy.Contains
                )
            }
        }
    }

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

                    val doUpdate = AliasStore.existsFrom(guild!!.id, from)

                    if (doUpdate) {
                        AliasStore.data.removeIf {
                            it.guildId == guild!!.id && it.from == from
                        }
                    }

                    AliasStore.add(AliasData(guild!!.id, user.id, type, from, to))

                    respond {
                        embed {
                            author {
                                name = user.asUser().username
                                icon = user.asUser().avatar?.url
                            }

                            title = ":loudspeaker: ${type.displayName}のエイリアスを${if (doUpdate) "更新" else "作成"}"

                            field(":mag: ${type.displayName}", true) {
                                when (type) {
                                    AliasType.Text -> from
                                    AliasType.Regex -> "`$from`"
                                    AliasType.Emoji -> "$from `$from`"
                                }
                            }

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                to
                            }

                            color = Color(0x7bda81)
                        }
                    }
                }
            }

            publicSubCommand(::UpdateOptions) {
                name = "update"
                description = "エイリアスを更新します。"

                action {
                    println(arguments.search)
                }
            }

            publicSubCommand {
                name = "delete"
                description = "エイリアスを削除します。"

                action {

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