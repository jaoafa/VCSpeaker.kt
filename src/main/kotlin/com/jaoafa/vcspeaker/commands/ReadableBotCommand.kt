package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.ReadableBotStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class ReadableBotCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class AddOptions : Options() {
        val user by user {
            name = "user"
            description = "読み上げを許可するBotのユーザー"
        }
    }

    inner class RemoveOptions : Options() {
        val user by user {
            name = "user"
            description = "読み上げを許可しなくなるBotのユーザー"
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("readablebot", "読み上げを許可するBotを設定します。") {
            check { anyGuild() }
            publicSubCommand("add", "読み上げを許可するBotを追加します。", ::AddOptions) {
                action {
                    val guildId = guild!!.id
                    val targetUser = arguments.user

                    if (ReadableBotStore.isReadableBot(guildId, targetUser)) {
                        respondEmbed(
                            ":speaking_head: Already Added",
                            "${targetUser.mention} は既に読み上げを許可するBotに追加されています。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableBotStore.add(guildId, targetUser, user.id)
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
                    val guildId = guild!!.id
                    val targetUser = arguments.user

                    if (!ReadableBotStore.isReadableBot(guildId, targetUser)) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "${targetUser.mention} は読み上げを許可するBotに追加されていません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableBotStore.remove(guildId, targetUser)
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
                    val guildId = guild!!.id
                    val readableBots = ReadableBotStore.data.filter { it.guildId == guildId }

                    if (readableBots.isEmpty()) {
                        respondEmbed(
                            ":speaking_head: No Readable Bots",
                            "このサーバーには読み上げを許可するBotが設定されていません。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                        return@action
                    }

                    val description = readableBots.joinToString("\n") { data ->
                        "<@${data.userId}> (Added by <@${data.addedByUserId}>)"
                    }

                    respondEmbed(
                        ":speaking_head: Readable Bots",
                        description
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }
        }
    }
}