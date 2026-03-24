package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.database.actions.GuildAction.isAutoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.leave
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tools.discord.isNotSelf
import com.jaoafa.vcspeaker.tools.discord.isVoiceTextChannelSet
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.coroutines.flow.count

class VoiceLeaveEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user left a voice channel
            check {
                isNotSelf()
                anyGuildRegistered()
                isVoiceTextChannelSet()

                // ボイスチャンネル退出時のみ
                failIf(event.old?.getChannelOrNull() == null || event.state.getChannelOrNull() != null)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val isBot = member.isBot
                val channelLeft = event.old?.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()
                val selfChannelCount = selfChannel?.voiceStates?.count { !it.getMember().isBot }

                val autoJoin = guild.isAutoJoinEnabled()

                // afk channel related
                if (channelLeft.isAfk()) {
                    guild.announce(
                        voice = NarrationScripts.userLeftOtherChannel(event.state.getMember(), channelLeft),
                        text = ":outbox_tray: `@${member.username}` が AFK から退出しました。",
                        isMessageOnly = isBot
                    )
                    return@action
                }

                // normal channel related
                if (autoJoin) {
                    // if the current channel is empty, leave the channel.
                    // if VCSpeaker not connected to any channel or the channel is not empty, do nothing.
                    if (selfChannelCount == 0) selfChannel.leave()
                }

                guild.announce(
                    voice = if (channelLeft == selfChannel) {
                        NarrationScripts.userLeft(member)
                    } else {
                        NarrationScripts.userLeftOtherChannel(member, channelLeft)
                    },
                    text = ":outbox_tray: `@${member.username}` が ${channelLeft.mention} から退出しました。",
                    isMessageOnly = isBot
                )
            }
        }
    }
}