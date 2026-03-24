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

class GoLiveStartEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                isNotBot()
                anyGuildRegistered()
                isVoiceTextChannelSet()

                // VCに参加したときではないこと
                failIf(event.old?.isSelfStreaming == null)
                // GoLiveのステータスが変わったときのみ
                failIf(event.old?.isSelfStreaming == event.state.isSelfStreaming)
                // GoLiveを開始したときのみ
                failIfNot(event.state.isSelfStreaming)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelGoLiveStarted = event.state.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()

                val voice = if (channelGoLiveStarted == selfChannel) {
                    NarrationScripts.userStartGoLive(member)
                } else {
                    NarrationScripts.userStartGoLiveOtherChannel(member, channelGoLiveStarted)
                }

                val goLiveRate = channelGoLiveStarted.getGoLiveRate()

                guild.announce(
                    voice = voice,
                    text = ":satellite: `@${member.username}` が ${channelGoLiveStarted.mention} で GoLive を開始しました。"
                            + if (goLiveRate > 0) " (GoLive 率: $goLiveRate%)" else ""
                )

                logger.info { "[${guild.name}] GoLive Started: @${member.username} Started GoLive (GoLive Rate: $goLiveRate%)" }
            }
        }
    }
}