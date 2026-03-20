package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntity
import com.jaoafa.vcspeaker.database.DatabaseUtil.getRows
import com.jaoafa.vcspeaker.database.tables.ReadableBotEntity as Entity
import com.jaoafa.vcspeaker.database.tables.ReadableBotTable as Table
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.isGuildRegistered
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.api.ErrorCode.DUPLICATE_KEY_1
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ReadableBotCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    class AddOptions : Options() {
        val user by user {
            name = "user"
            description = "読み上げを許可するBotのユーザー"
        }
    }

    class RemoveOptions : Options() {
        val user by user {
            name = "user"
            description = "読み上げを許可しなくなるBotのユーザー"
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("readablebot", "読み上げを許可するBotを設定します。") {
            check { isGuildRegistered() }
            publicSubCommand("add", "読み上げを許可するBotを追加します。", ::AddOptions) {
                action {
                    val guild = guild ?: return@action
                    val targetUser = arguments.user

                    try {
                        transaction {
                            Entity.new {
                                this.guildEntity = guild.getEntity()
                                this.botDid = targetUser.id
                                this.creatorDid = user.id
                            }
                        }
                    } catch (e: ExposedSQLException) {
                        when (e.sqlState.toInt()) {
                            DUPLICATE_KEY_1 -> {
                                respondEmbed(
                                    ":speaking_head: Already Added",
                                    "${targetUser.mention} は既に読み上げを許可するBotに追加されています。"
                                ) {
                                    authorOf(user)
                                    errorColor()
                                }

                                return@action
                            }

                            else -> throw e
                        }
                    }

                    respondEmbed(
                        ":speaking_head: Added Readable Bot",
                        "${targetUser.mention} を読み上げを許可するBotに追加しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Readable Bot Added: @${user.username} added readable bot ${targetUser.username} (${targetUser.id})"
                    }
                }
            }

            publicSubCommand("remove", "読み上げを許可するBotを削除します。", ::RemoveOptions) {
                action {
                    val guild = guild ?: return@action
                    val targetUser = arguments.user

                    val entity = transaction {
                        Entity.find {
                            (Table.guildDid eq guild.id) and (Table.botDid eq targetUser.id)
                        }.singleOrNull()
                    }

                    if (entity == null) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "${targetUser.mention} は読み上げを許可するBotに追加されていません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    transaction {
                        entity.delete()
                    }

                    respondEmbed(
                        ":face_with_symbols_over_mouth: Removed Readable Bot",
                        "${targetUser.mention} を読み上げを許可するBotから削除しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Readable Bot Removed: @${user.username} removed readable bot ${targetUser.username} (${targetUser.id})"
                    }
                }
            }

            publicSubCommand("list", "読み上げを許可するBotの一覧を表示します.") {
                action {
                    val guild = guild ?: return@action
                    val rows = transaction {
                        Entity.find { Table.guildDid eq guild.id }.getRows()
                    }

                    if (rows.isEmpty()) {
                        respondEmbed(
                            ":speaking_head: No Readable Bots",
                            "このサーバーには読み上げを許可するBotが設定されていません。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                        return@action
                    }

                    respondEmbed(
                        ":speaking_head: Readable Bots",
                        rows.joinToString("\n") { it.describe() }
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }
        }
    }
}