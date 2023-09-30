package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.Discord.selfVoiceChannel
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.announce
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.user.VoiceStateUpdateEvent

class EndGoLiveEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                // Botではないこと
                failIf(event.state.getMember().isBot)
                // 読み上げチャンネルが設定されていること
                val settings = GuildStore.getOrDefault(event.state.guildId)
                failIf(settings.channelId == null)
                // VCに参加したときではないこと
                failIf(event.old?.isSelfStreaming == null)
                // GoLiveのステータスが変わったときのみ
                failIf(event.old?.isSelfStreaming == event.state.isSelfStreaming)
                // GoLiveを終了したときのみ
                failIf(event.state.isSelfStreaming)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelGoLiveStarted = event.state.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()

                val voice = if (channelGoLiveStarted == selfChannel) {
                    NarrationScripts.userEndGoLive(member)
                } else {
                    NarrationScripts.userEndGoLiveOtherChannel(member, channelGoLiveStarted)
                }
                guild.announce(
                    voice = voice,
                    text = ":satellite:  g `@${member.username}` が ${channelGoLiveStarted.mention} でGoLiveを終了しました。"
                )
            }
        }
    }
}