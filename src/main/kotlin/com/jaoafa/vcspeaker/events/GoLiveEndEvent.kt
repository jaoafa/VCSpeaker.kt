package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.announce
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.user.VoiceStateUpdateEvent

class GoLiveEndEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                // Botではないこと
                failIf(event.state.getMember().isBot)
                // 読み上げチャンネルが設定されていること
                val settings = GuildStore.getOrDefault(event.state.guildId)
                failIf(settings.channelId == null)

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
                guild.announce(
                    voice = voice,
                    text = ":satellite: `@${member.username}` が ${channelGoLiveEnded.mention} で GoLive を終了しました。"
                )
            }
        }
    }
}