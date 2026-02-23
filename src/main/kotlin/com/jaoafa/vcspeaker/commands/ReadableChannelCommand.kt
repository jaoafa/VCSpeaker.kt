package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.ReadableChannelStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.converters.impl.channel
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.permissionsForMember
import io.github.oshai.kotlinlogging.KotlinLogging

class ReadableChannelCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class AddOptions : Options() {
        val channel by channel {
            name = "channel"
            description = "メッセージ内容の読み上げを許可するテキストチャンネル"
            requiredChannelTypes += ChannelType.GuildText
        }
    }

    inner class RemoveOptions : Options() {
        val channel by channel {
            name = "channel"
            description = "メッセージ内容の読み上げを許可するテキストチャンネル"
            requiredChannelTypes += ChannelType.GuildText
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("readablechannel", "メッセージ内容の読み上げを許可するテキストチャンネルを設定します。") {
            check { anyGuild() }
            publicSubCommand("add", "メッセージ内容の読み上げを許可するテキストチャンネルを追加します。", ::AddOptions) {
                action {
                    val guildId = guild!!.id
                    val targetChannel = arguments.channel

                    val targetTextChannel = targetChannel.asChannelOfOrNull<TextChannel>()
                    if (targetTextChannel == null) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Invalid Channel",
                            "${targetChannel.mention} は有効なテキストチャンネルではありません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    if (ReadableChannelStore.isReadableChannel(guildId, targetTextChannel)) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "${targetTextChannel.mention} はメッセージ内容の読み上げを許可するテキストチャンネルにすでに追加されています。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    // コマンド実行者が対象テキストチャンネルの管理権限を持っているか確認
                    val member = guild?.getMember(user.id)
                    if (member == null || targetTextChannel.permissionsForMember(member)
                            .contains(Permission.ManageChannels).not()
                    ) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Insufficient Permissions",
                            "この操作を実行するには、${targetTextChannel.mention} の管理権限が必要です。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableChannelStore.add(guildId, targetTextChannel, user.id)
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
                action {
                    val guildId = guild!!.id
                    val targetChannel = arguments.channel

                    val targetTextChannel = targetChannel.asChannelOfOrNull<TextChannel>()
                    if (targetTextChannel == null) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Invalid Channel",
                            "${targetChannel.mention} は有効なテキストチャンネルではありません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    if (!ReadableChannelStore.isReadableChannel(guildId, targetTextChannel)) {
                        respondEmbed(
                            ":face_with_symbols_over_mouth: Not Found",
                            "${targetChannel.mention} はメッセージ内容の読み上げを許可するテキストチャンネルに追加されていません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    ReadableChannelStore.remove(guildId, targetTextChannel)
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

            publicSubCommand("list", "メッセージ内容の読み上げを許可するテキストチャンネルの一覧を表示します.") {
                action {
                    val guildId = guild!!.id
                    val readableChannels = ReadableChannelStore.data.filter { it.guildId == guildId }

                    if (readableChannels.isEmpty()) {
                        respondEmbed(
                            ":speaking_head: No Readable Channels",
                            "このサーバーにはメッセージ内容の読み上げを許可するテキストチャンネルが設定されていません。"
                        ) {
                            authorOf(user)
                            successColor()
                        }
                        return@action
                    }

                    val descriptionLines = readableChannels.map { data ->
                        val channel = guild?.getChannelOrNull(data.channelId)
                        if (channel != null) {
                            "${channel.mention} (Added by <@${data.addedByUserId}>)"
                        } else {
                            "Unknown Channel (ID: ${data.channelId}, Added by <@${data.addedByUserId}>)"
                        }
                    }
                    val description = descriptionLines.joinToString("\n")

                    respondEmbed(
                        ":speaking_head: Readable Channels",
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