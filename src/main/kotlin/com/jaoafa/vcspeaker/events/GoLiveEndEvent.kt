package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.getGoLiveRate
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tools.discord.isVoiceTextChannelSet
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import io.github.oshai.kotlinlogging.KotlinLogging

class GoLiveEndEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                isNotBot()
                anyGuildRegistered()
                isVoiceTextChannelSet()

                val oldStreaming = event.old?.isSelfStreaming ?: false
                val newStreaming = event.state.isSelfStreaming
                val userLeft = event.old?.getChannelOrNull() != null && event.state.getChannelOrNull() == null

                // GoLive を終了 or GoLive をしたまま VC から退出
                if ((oldStreaming && !newStreaming) || (oldStreaming && userLeft)) {
                    pass()
                } else fail()
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelGoLiveEnded = event.old?.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()

                val voice = if (channelGoLiveEnded == selfChannel) {
                    NarrationScripts.userEndGoLive(member)
                } else {
                    NarrationScripts.userEndGoLiveOtherChannel(member, channelGoLiveEnded)
                }

                val goLiveRate = channelGoLiveEnded.getGoLiveRate()

                guild.announce(
                    voice = voice,
                    text = ":satellite: `@${member.username}` が ${channelGoLiveEnded.mention} で GoLive を終了しました。"
                            + if (goLiveRate > 0) " (GoLive 率: $goLiveRate%)" else ""
                )

                logger.info { "[${guild.name}] GoLive Stopped: @${member.username} Stopped GoLive (GoLive Rate: $goLiveRate%)" }
            }
        }
    }
}