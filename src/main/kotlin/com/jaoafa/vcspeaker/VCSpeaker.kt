package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.voicetext.GuildNarrator
import com.jaoafa.vcspeaker.voicetext.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import java.io.File

object VCSpeaker {
    lateinit var instance: ExtensibleBot
    lateinit var voiceText: VoiceTextAPI
    lateinit var config: Config

    var cachePolicy: Int = 7

    var dev: Snowflake? = null
    fun isDev() = dev != null

    val lavaplayer = DefaultAudioPlayerManager()
    val narrators = hashMapOf<Snowflake, GuildNarrator>()

    object Files {
        val config = File("config.yml")
        val cacheFolder = File("cache")
        val caches = File("caches.json")
        val guilds = File("guilds.json")
        val ignores = File("ignores.json")
        val aliases = File("aliases.json")
        val voices = File("voices.json")
    }

    init {
        AudioSourceManagers.registerLocalSource(lavaplayer)
    }
}