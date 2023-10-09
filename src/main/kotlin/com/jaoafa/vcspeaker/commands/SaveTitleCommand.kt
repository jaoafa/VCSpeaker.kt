package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.saveTitle
import com.jaoafa.vcspeaker.features.Title.saveTitleAll
import com.jaoafa.vcspeaker.tools.Discord.authorOf
import com.jaoafa.vcspeaker.tools.Discord.errorColor
import com.jaoafa.vcspeaker.tools.Discord.orMembersCurrent
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.publicSubCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Discord.respondEmbed
import com.jaoafa.vcspeaker.tools.Discord.successColor
import com.jaoafa.vcspeaker.tools.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.VoiceChannel

class SaveTitleCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class SaveOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "タイトルを保存するチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("save-title", "タイトルをチャンネル名として保存します。") {

            publicSubCommand("all", "全てのチャンネルのタイトルを保存します。") {
                action {
                    val titles = guild!!.saveTitleAll(user)

                    if (titles.size == 1) {
                        val first = titles.entries.first()

                        respondEmbed(
                            ":inbox_tray: Title Saved",
                            """
                                <#${first.key.channelId}> のタイトル「${first.key.title}」を保存しました。
                                レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                            """.trimIndent()
                        ) {
                            authorOf(user)
                            successColor()
                        }
                    } else if (titles.size >= 2) {
                        val diffString =
                            titles.entries.joinToString("\n") { "<#${it.key.channelId}> :「${it.key.title}」" }

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
                }
            }

            publicSubCommand("channel", "指定されたチャンネルのタイトルを保存します。", ::SaveOptions) {
                action {
                    val channel = (arguments.channel?.asChannelOf<VoiceChannel>() orMembersCurrent member!!)
                        ?: run {
                            respond("**:question: VC に参加、または指定してください。**")
                            return@action
                        }

                    val (old, new) = channel.saveTitle(user)

                    if (old != null && new != null) {
                        respondEmbed(
                            ":inbox_tray: Title Saved",
                            """
                                ${channel.mention} のタイトル「${old.title}」を保存しました。
                                レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                            """.trimIndent()
                        ) {
                            authorOf(user)
                            successColor()
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