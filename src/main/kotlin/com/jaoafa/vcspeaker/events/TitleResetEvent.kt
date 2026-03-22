package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.features.Title
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.getName
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.count

class TitleResetEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                failIf(event.state.getMember().isBot)
                failIf(event.old?.getChannelOrNull() == null)
                failIf(event.old?.getChannelOrNull()?.voiceStates?.count { !it.getMember().isBot } != 0)
            }

            action {
                val member = event.state.getMember()
                val channel = event.old?.getChannelOrNull() ?: return@action
                val (old, new) = Title.resetTitleOf(channel, member) ?: return@action

                val textChannel = // fixme
                    GuildStore[event.state.guildId]?.channelId?.asChannelOf<TextChannel>() ?: return@action
                val voiceChannel = event.old?.getChannelOrNull()!!

                textChannel.createEmbed {
                    title = ":broom: Title Reset"
                    description = "${voiceChannel.mention} のタイトルはリセットされました。"

                    authorOf(member)

                    field(":regional_indicator_o: チャンネル名", true) {
                        "`${new.originalTitle}` (デフォルト)"
                    }

                    field(":white_medium_small_square: 旧タイトル", true) {
                        old?.title?.let { "`$it`" } ?: "未登録"
                    }

                    successColor()
                }

                val guildName = event.state.getGuild().name
                val voiceName = voiceChannel.getName()

                logger.info { "[$guildName] Auto Title Reset: Title of $voiceName has been reset due to empty channel" }
            }
        }
    }
}