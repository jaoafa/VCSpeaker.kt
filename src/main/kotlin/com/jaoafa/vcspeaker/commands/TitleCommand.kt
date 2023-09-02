package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.getTitleData
import com.jaoafa.vcspeaker.features.Title.resetTitle
import com.jaoafa.vcspeaker.features.Title.setTitle
import com.jaoafa.vcspeaker.tools.Discord.authorOf
import com.jaoafa.vcspeaker.tools.Discord.errorColor
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Discord.respondEmbed
import com.jaoafa.vcspeaker.tools.Discord.successColor
import com.jaoafa.vcspeaker.tools.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.VoiceChannel

class TitleCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class TitleOptions : Options() {
        val title by optionalString {
            name = "title"
            description = "設定するタイトル"
        }

        val channel by optionalChannel {
            name = "channel"
            description = "タイトルを設定するチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("title", "タイトルを設定します。", ::TitleOptions) {
            action {
                val title = arguments.title
                val channel = arguments.channel?.asChannelOf<VoiceChannel>()
                    ?: member!!.getVoiceStateOrNull()?.getChannelOrNull() as VoiceChannel?
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }
                val oldData = channel.getTitleData()
                val user = event.interaction.user

                if (title != null) { // set or update
                    val newData = channel.setTitle(title, user)
                    respondEmbed(
                        ":regional_indicator_t: Title Set",
                        "${channel.mention} から全員が退出したらリセットされます。"
                    ) {
                        authorOf(user)

                        field(":new: 新タイトル", true) {
                            "`$title`"
                        }

                        field(":white_medium_small_square: 旧タイトル", true) {
                            oldData?.title?.let { "`$it`" } ?: "`${newData.original}` (デフォルト)"
                        }

                        successColor()
                    }
                } else { // reset
                    val (_, newData) = channel.resetTitle(user)

                    if (newData != null) {
                        respondEmbed(
                            ":broom: Title Reset",
                            "${channel.mention} のタイトルはリセットされました。"
                        ) {
                            authorOf(user)

                            field(":regional_indicator_o: チャンネル名", true) {
                                "`${newData.original}`"
                            }

                            field(":white_medium_small_square: 旧タイトル", true) {
                                oldData?.title!!.let { "`$it`" }
                            }

                            successColor()
                        }
                    } else {
                        respondEmbed(
                            ":question: Failed to Reset Title",
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