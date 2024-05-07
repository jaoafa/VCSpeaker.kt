package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.getTitleData
import com.jaoafa.vcspeaker.features.Title.setTitle
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import io.github.oshai.kotlinlogging.KotlinLogging

class TitleCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

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
            check { anyGuild() }
            action {
                val title = arguments.title
                val channel = arguments.channel.orFallbackTo(member!!) {
                    respond(it)
                } ?: return@action

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

                val channelName = channel.asChannel().name

                log(logger) { guild, user ->
                    "[${guild.name}] Title Set: Title set by @${user.username} in $channelName to \"$title\""
                }
            }
        }
    }
}