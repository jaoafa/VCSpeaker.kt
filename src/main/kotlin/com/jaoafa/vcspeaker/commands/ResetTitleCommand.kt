package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Title
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import dev.kord.common.entity.ChannelType
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class ResetTitleCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    class TitleOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "タイトルをリセットするチャンネル"

            requireChannelType(ChannelType.GuildVoice)
        }
    }

    override suspend fun setup() {
        publicSlashCommand("reset-title", "タイトルをリセットします。", ::TitleOptions) {
            check { anyGuildRegistered() }
            action {
                val channel = arguments.channel.orFallbackTo(member!!) {
                    respond(it)
                } ?: return@action

                val (old, new) = Title.resetTitleOf(channel, user) ?: run {
                    respondEmbed(
                        ":question: Title Not Reset",
                        "${channel.mention} にはタイトルが設定されていません。"
                    ) {
                        authorOf(user)
                        errorColor()
                    }
                    return@action
                }

                respondEmbed(
                    ":broom: Title Reset",
                    """
                            ${channel.mention} のタイトルはリセットされました。
                            レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                        """.trimIndent()
                ) {
                    authorOf(user)

                    field(":regional_indicator_o: チャンネル名", true) {
                        "`${new.originalTitle}`"
                    }

                    field(":white_medium_small_square: 旧タイトル", true) {
                        old?.title.let { "`$it`" }
                    }

                    successColor()
                }

                val channelName = channel.asChannel().name

                log(logger) { guild, user ->
                    "[${guild.name}] Title Reset: @${user.username} reset the title of $channelName"
                }
            }
        }
    }
}
