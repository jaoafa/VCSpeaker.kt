package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.tools.Emoji
import com.jaoafa.vcspeaker.tts.api.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import java.io.File

object VCSpeaker {
    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord
    val lavaplayer = DefaultAudioPlayerManager()

    lateinit var voicetext: VoiceTextAPI
    lateinit var config: Config

    lateinit var storeFolder: File
    lateinit var cacheFolder: File

    var cachePolicy: Int = 7
    lateinit var prefix: String

    // 開発環境のコマンドを登録する Guild ID (null で開発環境を無効化)
    var devId: Snowflake? = null

    // 開発環境かどうか
    fun isDev() = devId != null

    // Store ファイルパス
    object Files {
        private operator fun File.plus(file: File) = File(this, file.name)

        val caches = storeFolder + File("caches.json")
        val guilds = storeFolder + File("guilds.json")
        val ignores = storeFolder + File("ignores.json")
        val aliases = storeFolder + File("aliases.json")
        val voices = storeFolder + File("voices.json")
        val titles = storeFolder + File("titles.json")
        val visionApiCounter = storeFolder + File("vision-api-counter.json")

        val visionApiCache = storeFolder + File("vision-api") + File("cache")
    }

    /**
     * VCSpeaker を初期化します。
     *
     * @param voicetext [VoiceTextAPI] インスタンス
     * @param config [Config]
     */
    fun init(
        voicetext: VoiceTextAPI,
        config: Config,
        storeFolder: File,
        cacheFolder: File,
        devId: Snowflake?,
        cachePolicy: Int?,
        prefix: String,
        resamplingQuality: ResamplingQuality,
        encodingQuality: Int
    ) {
        Emoji // init

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

        lavaplayer.configuration.let {
            it.resamplingQuality = resamplingQuality
            it.opusEncodingQuality = encodingQuality
        }
    }
}