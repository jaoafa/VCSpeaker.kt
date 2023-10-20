package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.getTitleData
import com.jaoafa.vcspeaker.features.Title.setTitle
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orMembersCurrent
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.Options
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.VoiceChannel

class TitleCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class TitleOptions : Options() {
        val title by string {
            name = "title"
            description = "設定するタイトル"
        }

        val channel by optionalChannel {
            name = "channel"
            description = "タイトルを設定するチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    // todo emoji detection
    override suspend fun setup() {
        publicSlashCommand("title", "タイトルを設定します。", ::TitleOptions) {
            action {
                val title = arguments.title
                val channel = arguments.channel?.asChannelOf<VoiceChannel>().orMembersCurrent(member!!)
                    ?: run {
                        respond("**:question: VC に参加、または指定してください。**")
                        return@action
                    }

                val oldData = channel.getTitleData()
                val newData = channel.setTitle(title, user)

                respondEmbed(
                    ":regional_indicator_t: Title Set",
                    """
                        ${channel.mention} から全員が退出したらリセットされます。
                        レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                    """.trimIndent()
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
            }
        }
    }
}