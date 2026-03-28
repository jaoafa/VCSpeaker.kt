package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.database.actions.GuildAction.getVoiceTextChannelOrNull
import com.jaoafa.vcspeaker.features.Title
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.getName
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kordex.core.checks.isNotBot
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
                anyGuildRegistered()
                isNotBot()
                failIf(event.old?.getChannelOrNull() == null)
                failIf(event.old?.getChannelOrNull()?.voiceStates?.count { !it.getMember().isBot } != 0)
            }

            action {
                val guild = event.state.getGuild()
                val member = event.state.getMember()
                val voiceChannel = event.old?.getChannelOrNull() ?: return@action

                val (old, new) = Title.resetTitleOf(voiceChannel, member) ?: return@action

                val textChannel = guild.getVoiceTextChannelOrNull() ?: return@action

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

                val voiceName = voiceChannel.getName()

                logger.info { "[${guild.name}] Auto Title Reset: Title of $voiceName has been reset due to empty channel" }
            }
        }
    }
}