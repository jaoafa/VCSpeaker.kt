package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.voicetext.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.voice.VoiceConnection

object VCSpeaker {
    lateinit var kord: ExtensibleBot
    lateinit var voiceText: VoiceTextAPI
    lateinit var config: Config

    val lavaplayer = DefaultAudioPlayerManager()

    val guildPlayer = hashMapOf<Snowflake, AudioPlayer>()

    @OptIn(KordVoice::class)
    val connections = hashMapOf<Snowflake, VoiceConnection>()

    init {
        AudioSourceManagers.registerLocalSource(lavaplayer)
    }
}