package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.actions.TitleAction
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.orFallbackTo
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager.getNarrator
import dev.kord.common.entity.ChannelType
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class TitleCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    class TitleOptions : Options() {
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
            check { anyGuildRegistered() }
            action {
                val title = arguments.title
                val channel = arguments.channel.orFallbackTo(member!!) {
                    respond(it)
                } ?: return@action

                val (old, new) = TitleAction.setTitleOf(channel, title, user)

                respondEmbed(
                    ":regional_indicator_t: Title Set",
                    """
                        タイトル「${new.title}」を ${channel.mention} に設定しました。
                        全員が退出したらリセットされます。
                        レートリミットにより、チャンネル名が反映されるまで時間がかかる場合があります。
                    """.trimIndent()
                ) {
                    authorOf(user)

                    field(":new: 新タイトル", true) {
                        "`$title`"
                    }

                    field(":white_medium_small_square: 旧タイトル", true) {
                        old?.title?.let { "`$it`" } ?: "`${new.originalTitle}` (デフォルト)"
                    }

                    successColor()
                }

                guild?.getNarrator()?.scheduleAsSystem("タイトルを「$title」に変更しました。")
                val channelName = channel.asChannel().name

                log(logger) { guild, user ->
                    "[${guild.name}] Title Set: Title set by @${user.username} in $channelName to \"$title\""
                }
            }
        }
    }
}
