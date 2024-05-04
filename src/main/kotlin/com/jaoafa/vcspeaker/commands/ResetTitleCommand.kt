package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title.resetTitle
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.ChannelType
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("unused")
class ResetTitleCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class TitleOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "タイトルをリセットするチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("reset-title", "タイトルをリセットします。", ::TitleOptions) {
            check { anyGuild() }
            action {
                val channel = arguments.channel.orFallbackTo(member!!) {
                    respond(it)
                } ?: return@action

                val (oldData, newData) = channel.resetTitle(user)

                if (newData != null) {
                    respondEmbed(
                        ":broom: Title Reset",
                        """
                            ${channel.mention} のタイトルはリセットされました。
                            レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                        """.trimIndent()
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

                    val channelName = channel.asChannel().name

                    log(logger) { guild, user ->
                        "[${guild.name}] Title Reset: @${user.username} reset the title of $channelName"
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