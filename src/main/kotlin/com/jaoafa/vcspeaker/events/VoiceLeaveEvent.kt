package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.leave
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.autoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tts.NarrationScripts
import com.jaoafa.vcspeaker.tts.NarratorExtensions.announce
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.user.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.count

class VoiceLeaveEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user left a voice channel
            check {
                failIf(event.state.getMember().isBot)
                val settings = GuildStore.getOrDefault(event.state.guildId)
                failIf(settings.channelId == null)
                failIf(event.old?.getChannelOrNull() == null || event.state.getChannelOrNull() != null)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelLeft = event.old?.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()
                val selfChannelCount = selfChannel?.voiceStates?.count { !it.getMember().isBot }

                val autoJoin = guild.autoJoinEnabled()

                // afk channel related
                if (channelLeft.isAfk()) {
                    guild.announce(
                        voice = NarrationScripts.userLeftOtherChannel(event.state.getMember(), channelLeft),
                        text = ":outbox_tray: `@${member.username}` が AFK から退出しました。"
                    )
                    return@action
                }

                // normal channel related
                if (autoJoin) {
                    // if current channel is empty, leave the channel.
                    // if VCSpeaker not connected to any channel or the channel is not empty, do nothing.
                    if (selfChannelCount == 0) selfChannel.leave()
                }

                guild.announce(
                    voice = if (channelLeft == selfChannel) NarrationScripts.userLeft(member)
                    else NarrationScripts.userLeftOtherChannel(member, channelLeft),
                    text = ":outbox_tray: `@${member.username}` が ${channelLeft.mention} から退出しました。"
                )
            }
        }
    }
}