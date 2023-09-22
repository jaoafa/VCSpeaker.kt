package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.getTitleData
import com.jaoafa.vcspeaker.features.Title.resetTitle
import com.jaoafa.vcspeaker.tools.Discord.authorOf
import com.jaoafa.vcspeaker.tools.Discord.errorColor
import com.jaoafa.vcspeaker.tools.Discord.orMembersCurrent
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.jaoafa.vcspeaker.tools.Discord.respondEmbed
import com.jaoafa.vcspeaker.tools.Discord.successColor
import com.jaoafa.vcspeaker.tools.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.VoiceChannel

class ResetTitleCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class TitleOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "タイトルをリセットするチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("reset-title", "タイトルをリセットします。", ::TitleOptions) {
            action {
                val channel = arguments.channel?.asChannelOf<VoiceChannel>().orMembersCurrent(member!!)
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }

                val oldData = channel.getTitleData()
                val user = event.interaction.user

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