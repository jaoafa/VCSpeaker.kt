package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.GuildStore
import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.Narrator
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.speakSelf
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.annotation.KordVoice
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.voice.AudioFrame
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter

class AutoJoinEvent : Extension() {
    override val name = this::class.simpleName!!

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user joined (or moved to) a voice channel
            check {
                failIf(event.state.getMember().isBot)
                val settings = GuildStore.getOrDefault(event.state.guildId)
                failIf(!settings.autoJoin)
                failIf(settings.channelId == null)
                failIf(event.state.getChannelOrNull() == null)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val channel = event.state.getChannelOrNull() ?: return@action // checked
                val selfChannel = guild.selfMember().getVoiceStateOrNull()?.getChannelOrNull()
                val textChannel =
                    GuildStore.getOrDefault(guild.id).channelId?.let { kord.getChannelOf<TextChannel>(it) }

                if (selfChannel != null) { // move
                    // if current channel is empty, move to the channel
                    val narrator = VCSpeaker.narrators[guild.id] ?: return@action
                    if (selfChannel.voiceStates.filter { !it.getMember().isBot }.count() == 0) {
                        narrator.connection.move(channel.id)

                        narrator.player.speakSelf(NarrationScripts.SELF_MOVE, guild.id)
                        textChannel?.createMessage("**:arrow_right: ${channel.mention} に移動しました。**")
                    }
                } else { // join
                    // force disconnection
                    if (guild.selfMember().getVoiceStateOrNull()?.getChannelOrNull() == null)
                        VCSpeaker.narrators.remove(guild.id)

                    val player = VCSpeaker.lavaplayer.createPlayer()

                    player.speakSelf(NarrationScripts.SELF_JOIN, guild.id) // DO NOT REMOVE

                    val connection = (channel as VoiceChannelBehavior).connect {
                        audioProvider {
                            AudioFrame.fromData(player.provide()?.data ?: ByteArray(0))
                        }
                    }

                    VCSpeaker.narrators[guild.id] = Narrator(guild.id, player, connection)

                    textChannel?.createMessage("**:arrow_right: ${channel.mention} に接続しました。**")
                }
            }
        }
    }
}