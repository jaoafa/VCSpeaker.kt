package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.features.Title.resetTitle
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.Discord.asChannelOf
import com.jaoafa.vcspeaker.tools.Discord.authorOf
import com.jaoafa.vcspeaker.tools.Discord.successColor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.count

class TitleEvent : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                failIf(event.state.getMember().isBot)
                failIf(event.old?.getChannelOrNull() == null || event.state.getChannelOrNull() != null)
                failIf(event.old?.getChannelOrNull()?.voiceStates?.count { !it.getMember().isBot } != 0)
            }

            action {
                val user = event.state.getMember()

                val (oldData, newData) = event.old!!.getChannelOrNull()!!.resetTitle(user) // checked

                if (newData == null) return@action

                val textChannel =
                    GuildStore[event.state.guildId]?.channelId?.asChannelOf<TextChannel>() ?: return@action

                textChannel.createEmbed {
                    title = ":broom: Title Reset"
                    description = "${textChannel.mention} のタイトルはリセットされました。"

                    authorOf(user)

                    field(":regional_indicator_o: チャンネル名", true) {
                        "`${newData.original}` (デフォルト)"
                    }

                    field(":white_medium_small_square: 旧タイトル", true) {
                        oldData?.title!!.let { "`$it`" }
                    }

                    successColor()
                }
            }
        }
    }
}