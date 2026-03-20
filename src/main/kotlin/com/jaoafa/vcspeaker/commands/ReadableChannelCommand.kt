package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntity
import com.jaoafa.vcspeaker.database.DatabaseUtil.getRows
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbedOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.EmbedTemplates
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.isGuildRegistered
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor
import dev.kordex.core.commands.converters.impl.channel
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.permissionsForMember
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.api.ErrorCode.DUPLICATE_KEY_1
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.jaoafa.vcspeaker.database.tables.ReadableChannelEntity as Entity
import com.jaoafa.vcspeaker.database.tables.ReadableChannelTable as Table

class ReadableChannelCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    class AddOptions : Options() {
        val channel by channel {
            name = "channel"
            description = "メッセージ内容の読み上げを許可するテキストチャンネル"
            requiredChannelTypes += ChannelType.GuildText
        }
    }

    class RemoveOptions : Options() {
        val channel by channel {
            name = "channel"
            description = "メッセージ内容の読み上げを行わなくするテキストチャンネル"
            requiredChannelTypes += ChannelType.GuildText
        }
    }

    private suspend fun CheckContext<ChatInputCommandInteractionCreateEvent>.hasChannelPermission() {
        val guild = guildFor(event) ?: return
        val user = userFor(event)?.asUser() ?: return
        val channel = event.interaction.command.channels["channel"]?.asChannelOf<TextChannel>() ?: return

        // コマンド実行者が対象テキストチャンネルの管理権限を持っているか確認
        val member = guild.getMember(user.id)
        if (channel
                .permissionsForMember(member)
                .contains(Permission.ManageChannels).not()
        ) {
            event.interaction.respondEphemeral {
                embed(EmbedTemplates.InsufficientChannelPermissions(channel).build {
                    authorOf(user)
                })
            }

            fail()
            return
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("readablechannel", "メッセージ内容の読み上げを許可するテキストチャンネルを設定します。") {
            check { isGuildRegistered() }
            publicSubCommand("add", "メッセージ内容の読み上げを許可するテキストチャンネルを追加します。", ::AddOptions) {
                check { hasChannelPermission() }
                action {
                    val guild = guild ?: return@action
                    val targetChannel = arguments.channel

                    val targetTextChannel = targetChannel.asChannelOfOrNull<TextChannel>()
                    if (targetTextChannel == null) {
                        respondEmbedOf(EmbedTemplates.InvalidChannel(targetChannel).buildSuspended {
                            authorOf(user)
                        })
                        return@action
                    }

                    try {
                        transaction {
                            Entity.new {
                                this.guildEntity = guild.getEntity()
                                this.channelDid = targetChannel.id
                                this.creatorDid = user.id
                            }
                        }
                    } catch (e: ExposedSQLException) {
                        when (e.sqlState.toInt()) {
                            DUPLICATE_KEY_1 -> {
                                respondEmbed(
                                    ":speaking_head: Already Added",
                                    "${targetTextChannel.mention} はメッセージ内容の読み上げを許可するテキストチャンネルに既に追加されています。"
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
                        ":speaking_head: Added Readable Channel",
                        "${targetTextChannel.mention} についてメッセージ内容の読み上げを許可するテキストチャンネルに追加しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Readable Channel Added: @${user.username} added readable channel ${targetTextChannel.name} (${targetChannel.id})"
                    }
                }
            }

            publicSubCommand(
                "remove",
                "メッセージ内容の読み上げを許可するテキストチャンネルを削除します。",
                ::RemoveOptions
            ) {
                check { hasChannelPermission() }
                action {
                    val guild = guild ?: return@action
                    val targetChannel = arguments.channel

                    val entity = transaction {
                        Entity.find {
                            (Table.guildDid eq guild.id) and (Table.channelDid eq targetChannel.id)
                        }.singleOrNull()
                    }

                    val targetTextChannel = targetChannel.asChannelOfOrNull<TextChannel>()
                    if (targetTextChannel == null) {
                        respondEmbedOf(EmbedTemplates.InvalidChannel(targetChannel).buildSuspended {
                            authorOf(user)
                        })
                        return@action
                    }

                    if (entity == null) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "${targetChannel.mention} はメッセージ内容の読み上げを許可するテキストチャンネルに追加されていません。"
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
                        ":face_with_symbols_over_mouth: Removed Readable Channel",
                        "${targetChannel.mention} をメッセージ内容の読み上げを許可するテキストチャンネルから削除しました。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Readable Channel Removed: @${user.username} removed readable channel ${targetTextChannel.name} (${targetChannel.id})"
                    }
                }
            }

            publicSubCommand("list", "メッセージ内容の読み上げを許可するテキストチャンネルの一覧を表示します。") {
                action {
                    val guild = guild ?: return@action

                    val entities = transaction {
                        Entity.find { Table.guildDid eq guild.id }.getRows()
                    }

                    if (entities.isEmpty()) {
                        respondEmbed(
                            ":speaking_head: No Readable Channels",
                            "このサーバーにはメッセージ内容の読み上げを許可するテキストチャンネルが設定されていません。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                        return@action
                    }

                    respondEmbed(
                        ":speaking_head: Readable Channels",
                        entities.joinToString("\n") { it.describe() }
                    ) {
                        authorOf(user)
                        successColor()
                    }
                }
            }
        }
    }
}