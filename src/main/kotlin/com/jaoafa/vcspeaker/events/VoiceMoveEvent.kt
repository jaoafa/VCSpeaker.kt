package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.leave
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.move
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.autoJoinEnabled
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.core.event.user.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.count

class VoiceMoveEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user moved from one to another voice channel
            check {
                failIf(event.state.getMember().isBot)

                val settings = GuildStore.getOrDefault(event.state.guildId)

                failIf(settings.channelId == null)

                val channelJoined = event.state.getChannelOrNull()
                val channelLeft = event.old?.getChannelOrNull()

                failIf(channelJoined == null || channelLeft == null)
                failIf(channelJoined == channelLeft)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelJoined = event.state.getChannelOrNull() ?: return@action // checked
                val channelLeft = event.old?.getChannelOrNull() ?: return@action // checked

                val selfVoiceState = guild.selfMember().getVoiceStateOrNull()
                val selfChannel = selfVoiceState?.getChannelOrNull()
                val selfChannelCount = selfChannel?.voiceStates?.count { !it.getMember().isBot }
                val selfConnected = selfChannel != null

                val autoJoin = guild.autoJoinEnabled()

                // afk channel related
                if (channelJoined.isAfk()) { // ? -> afk
                    // if current channel is empty, leave the channel
                    if (autoJoin && selfChannelCount == 0) selfChannel.leave()

                    guild.announce(
                        voice = NarrationScripts.userAfk(member),
                        text = ":zzz: `@${member.username}` が AFK になりました。"
                    )

                    return@action
                } else if (channelLeft.isAfk()) { // afk -> ?
                    // if VCSpeaker not connected to any channel, join the channel
                    if (autoJoin && !selfConnected) channelJoined.join()

                    guild.announce(
                        voice = if (channelJoined == selfChannel) NarrationScripts.userAfkReturned(member)
                        else NarrationScripts.userAfkReturnedOtherChannel(member, channelJoined),
                        text = ":arrow_right: `@${member.username}` が AFK から ${channelJoined.mention} へ戻りました。"
                    )

                    return@action
                }

                // normal channel related
                if (autoJoin) {
                    // if current channel is empty, move to the channel
                    if (selfChannelCount == 0) channelJoined.move()
                    // if VCSpeaker not connected to any channel, join the channel
                    else if (!selfConnected) channelJoined.join()
                }

                guild.announce(
                    voice = if (channelJoined == selfChannel) NarrationScripts.userJoined(member)
                    else NarrationScripts.userJoinedOtherChannel(member, channelJoined),
                    text = ":arrow_right: `@${member.username}` が ${channelLeft.mention} から ${channelJoined.mention} へ移動しました。"
                )
            }
        }
    }
}