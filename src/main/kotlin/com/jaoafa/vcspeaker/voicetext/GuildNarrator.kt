package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.store.GuildStore
import com.jaoafa.vcspeaker.store.VoiceStore
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.voice.VoiceConnection

class GuildNarrator @OptIn(KordVoice::class) constructor(
    val guildId: Snowflake,
    val player: AudioPlayer,
    val connection: VoiceConnection
) {
    val scheduler = NarratorScheduler(guildId, player)

    suspend fun queue(text: String, voice: Voice) = scheduler.queue(SpeakInfo(text, voice))

    suspend fun skip() = scheduler.skip()

    suspend fun queueSelf(text: String) =
        scheduler.queue(SpeakInfo(text, GuildStore.getOrDefault(guildId).voice))

    suspend fun queueUser(text: String, userId: Snowflake, message: Message) =
        scheduler.queue(SpeakInfo(text, VoiceStore.byIdOrDefault(userId), message))

    init {
        player.addListener(scheduler)
    }
}