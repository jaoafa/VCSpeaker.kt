package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.ReadableBotStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kord.common.entity.Snowflake
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.FilterStrategy
import dev.kordex.core.utils.suggestStringCollection
import io.github.oshai.kotlinlogging.KotlinLogging

class ReadableBotCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class AddOptions : Options() {
        val userId by string {
            name = "userid"
            description = "読み上げを許可するBotのユーザーID"
        }
    }

    inner class RemoveOptions : Options() {
        val userId by string {
            name = "userid"
            description = "読み上げを許可しなくなるBotのユーザーID"

            autoComplete { event ->
                val guildId = event.interaction.getChannel().data.guildId.value

                suggestStringCollection(
                    ReadableBotStore.filter(guildId).map { it.userId.toString() },
                    FilterStrategy.Contains
                )
            }
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("readablebot", "読み上げを許可するBotを設定します。") {
            check { anyGuild() }
            publicSubCommand("add", "読み上げを許可するBotを追加します。", ::AddOptions) {
                action {
                    val guildId = guild!!.id
                    val userId = Snowflake(arguments.userId)

                    if (ReadableBotStore.isReadableBot(guildId, userId)) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Already Added",
                            "既に読み上げを許可するBotに追加されています。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableBotStore.add(guildId, userId, user.id)
                    respondEmbed(
                        ":face_with_symbols_over_mouth: Added Readable Bot",
                        "読み上げを許可するBotに追加しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }

            publicSubCommand("remove", "読み上げを許可するBotを削除します。", ::RemoveOptions) {
                action {
                    val guildId = guild!!.id
                    val userId = Snowflake(arguments.userId)

                    if (!ReadableBotStore.isReadableBot(guildId, userId)) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "読み上げを許可するBotに追加されていません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableBotStore.remove(guildId, userId)
                    respondEmbed(
                        ":face_with_symbols_over_mouth: Removed Readable Bot",
                        "読み上げを許可するBotから削除しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }
        }
    }
}