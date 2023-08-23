package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.GuildStore
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.annotation.KordVoice
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter

class AutoLeaveEvent : Extension() {
    override val name = this::class.simpleName!!

    @OptIn(KordVoice::class)
    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            // auto-join enabled for the guild
            // target channel is set
            // user left a voice channel
            check {
                failIf(event.state.getMember().isBot)
                val settings = GuildStore.getOrDefault(event.state.guildId)
                failIf(!settings.autoJoin)
                failIf(settings.channelId == null)
                failIf(!(event.old?.getChannelOrNull() != null && event.state.getChannelOrNull() == null))
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val selfChannel = guild.selfMember().getVoiceStateOrNull()?.getChannelOrNull()
                val textChannel =
                    GuildStore.getOrDefault(guild.id).channelId?.let { kord.getChannelOf<TextChannel>(it) }

                if (selfChannel != null) { // leave
                    // if current channel is empty, leave the channel
                    val narrator = VCSpeaker.narrators[guild.id] ?: return@action
                    if (selfChannel.voiceStates.filter { !it.getMember().isBot }.count() == 0) {
                        narrator.connection.leave()
                        narrator.player.destroy()
                        VCSpeaker.narrators.remove(guild.id)
                        textChannel?.createMessage("**:wave: ${selfChannel.mention} から切断しました。**")
                    }
                }
            }
        }
    }
}