package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.saveTitle
import com.jaoafa.vcspeaker.features.Title.saveTitleAll
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.extensions.Extension
import dev.kord.common.entity.ChannelType
import io.github.oshai.kotlinlogging.KotlinLogging

class SaveTitleCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class SaveOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "タイトルを保存するチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("save-title", "タイトルをチャンネル名として保存します。") {
            check { anyGuild() }
            publicSubCommand("all", "全てのチャンネルのタイトルを保存します。") {
                action {
                    val titles = guild!!.saveTitleAll(user)

                    if (titles.size == 1) {
                        val first = titles.entries.first()

                        respondEmbed(
                            ":inbox_tray: Title Saved",
                            """
                                <#${first.value.channelId}> のタイトル「${first.value.original}」を保存しました。
                                レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                            """.trimIndent()
                        ) {
                            authorOf(user)
                            successColor()
                        }
                    } else if (titles.size >= 2) {
                        val diffString = titles.entries.joinToString("\n") {
                            "<#${it.value.channelId}> :「${it.value.original}」"
                        }

                        respondEmbed(
                            ":inbox_tray: All Titles Saved",
                            """
                                すべてのタイトルを保存しました。
                                $diffString
                                レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                            """.trimIndent()
                        ) {
                            authorOf(user)
                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: Failed to Save Titles",
                            "保存するタイトルが見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] All Titles Saved: @${user.username} saved ${titles.size} titles"
                    }
                }
            }

            publicSubCommand("channel", "指定されたチャンネルのタイトルを保存します。", ::SaveOptions) {
                action {
                    val channel = arguments.channel.orFallbackTo(member!!) {
                        respond(it)
                    } ?: return@action

                    val (old, new) = channel.saveTitle(user)

                    if (old != null && new != null) {
                        respondEmbed(
                            ":inbox_tray: Title Saved",
                            """
                                ${channel.mention} のタイトル「${new.original}」を保存しました。
                                レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                            """.trimIndent()
                        ) {
                            authorOf(user)
                            successColor()
                        }

                        val channelName = channel.asChannel().name

                        log(logger) { guild, user ->
                            "[${guild.name}] Title Saved: @${user.username} saved the title of $channelName"
                        }
                    } else {
                        respondEmbed(
                            ":question: Failed to Save Title",
                            "${channel.mention} にはタイトルが設定されていません。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                    }
                }
            }
        }
    }
}