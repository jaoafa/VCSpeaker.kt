package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.selfVoiceChannel
import com.jaoafa.vcspeaker.tts.narrators.NarrationScripts
import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.user.VoiceStateUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("unused")
class GoLiveStartEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

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
                // GoLiveを開始したときのみ
                failIfNot(event.state.isSelfStreaming)
            }

            action {
                val guild = event.state.getGuildOrNull() ?: return@action // checked
                val member = event.state.getMember()
                val channelGoLiveStarted = event.state.getChannelOrNull() ?: return@action // checked

                val selfChannel = guild.selfVoiceChannel()

                val voice = if (channelGoLiveStarted == selfChannel) {
                    NarrationScripts.userStartGoLive(member)
                } else {
                    NarrationScripts.userStartGoLiveOtherChannel(member, channelGoLiveStarted)
                }

                guild.announce(
                    voice = voice,
                    text = ":satellite: `@${member.username}` が ${channelGoLiveStarted.mention} で GoLive を開始しました。"
                )

                logger.info { "[${guild.name}] GoLive Started: @${member.username} Started GoLive-ing" }
            }
        }
    }
}