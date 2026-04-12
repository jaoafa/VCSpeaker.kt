package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.database.actions.GuildAction.isAutoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.move
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tools.discord.isNotSelf
import com.jaoafa.vcspeaker.tools.discord.isVoiceTextChannelSet
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.coroutines.flow.count

class VoiceJoinEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user joined a voice channel
            check {
                isNotSelf()
                anyGuildRegistered()
                isVoiceTextChannelSet()

                // ボイスチャンネル参加時のみ
                failIf(event.old?.getChannelOrNull() != null || event.state.getChannelOrNull() == null)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val isBot = member.isBot
                val channelJoined = event.state.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()
                val selfChannelCount = selfChannel?.voiceStates?.count { !it.getMember().isBot }
                val selfConnected = selfChannel != null

                val autoJoin = guild.isAutoJoinEnabled()

                // afk channel related
                if (channelJoined.isAfk()) {
                    guild.announce(
                        voice = NarrationScripts.userAfk(member),
                        text = ":zzz: `@${member.username}` が開幕初手 AFK をしました。",
                        isMessageOnly = isBot
                    )

                    return@action
                }

                // normal channel related
                if (autoJoin) {
                    // if the current channel is empty, move to the channel
                    if (selfChannelCount == 0) channelJoined.move()
                    // if VCSpeaker not connected to any channel, join the channel
                    else if (!selfConnected) channelJoined.join()
                }

                guild.announce(
                    voice = if (channelJoined == selfChannel) {
                        NarrationScripts.userJoined(member)
                    } else {
                        NarrationScripts.userJoinedOtherChannel(member, channelJoined)
                    },
                    text = ":inbox_tray: `@${member.username}` が ${channelJoined.mention} に参加しました。",
                    isMessageOnly = isBot
                )
            }
        }
    }
}