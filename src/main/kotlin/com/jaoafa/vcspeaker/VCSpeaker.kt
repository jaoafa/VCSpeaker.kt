package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.voicetext.api.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import java.io.File

object VCSpeaker {
    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord

    lateinit var voicetext: VoiceTextAPI
    lateinit var config: Config

    lateinit var storeFolder: File
    lateinit var cacheFolder: File

    var cachePolicy: Int = 7
    lateinit var prefix: String

    var devId: Snowflake? = null
    fun isDev() = devId != null

    val lavaplayer = DefaultAudioPlayerManager()

    object Files {
        private operator fun File.plus(file: File) = File(this, file.name)
        val caches = storeFolder + File("caches.json")
        val guilds = storeFolder + File("guilds.json")
        val ignores = storeFolder + File("ignores.json")
        val aliases = storeFolder + File("aliases.json")
        val voices = storeFolder + File("voices.json")
        val titles = storeFolder + File("titles.json")
    }

    fun init(
        voicetext: VoiceTextAPI,
        config: Config,
        storeFolder: File,
        cacheFolder: File,
        devId: Snowflake?,
        cachePolicy: Int?,
        prefix: String
    ) {
        AudioSourceManagers.registerLocalSource(lavaplayer)

        if (!storeFolder.exists()) storeFolder.mkdir()
        if (!cacheFolder.exists()) cacheFolder.mkdir()

        VCSpeaker.run {
            this.voicetext = voicetext
            this.config = config
            this.storeFolder = storeFolder
            this.cacheFolder = cacheFolder
            this.devId = devId
            this.cachePolicy = cachePolicy ?: 7
            this.prefix = prefix
        }
    }
}