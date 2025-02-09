package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.getSettings
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isAfk
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.join
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.VoiceStateUpdateEvent

class SelfVoiceJoinEvent : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                failIf(event.state.getMember().id != VCSpeaker.kord.selfId)
                failIf(!(event.state.getChannelOrNull()?.isAfk() ?: false))
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val channelLeft = event.old?.getChannelOrNull()
                val textChannel = guild.getSettings().channelId?.asChannelOf<TextChannel>()

                channelLeft?.join {
                    textChannel?.createMessage(
                        "**:zzz: AFK チャンネルには接続できないので、${channelLeft.mention} に戻りました。**"
                    )
                }
            }
        }
    }
}