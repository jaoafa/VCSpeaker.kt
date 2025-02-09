package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.tools.Emoji
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kordex.core.ExtensibleBot
import java.io.File

object VCSpeaker {
    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord
    val lavaplayer = DefaultAudioPlayerManager()

    lateinit var voicetextToken: String
    lateinit var config: Config

    lateinit var storeFolder: File
    lateinit var cacheFolder: File

    lateinit var prefix: String

    // 開発環境のコマンドを登録する Guild ID (null で開発環境を無効化)
    var devGuildId: Snowflake? = null

    // 開発環境かどうか
    fun isDev() = devGuildId != null

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
     * @param voicetextToken [VoiceTextProvider] インスタンス
     * @param config [Config]
     */
    fun init(
        config: Config,
        voicetextToken: String,
        storeFolder: File,
        cacheFolder: File,
        devGuildId: Snowflake?,
        prefix: String,
        resamplingQuality: ResamplingQuality,
        encodingQuality: Int
    ) {
        Emoji // init

        AudioSourceManagers.registerLocalSource(lavaplayer)

        if (!storeFolder.exists()) storeFolder.mkdir()
        if (!cacheFolder.exists()) cacheFolder.mkdir()

        VCSpeaker.run {
            this.voicetextToken = voicetextToken
            this.config = config
            this.storeFolder = storeFolder
            this.cacheFolder = cacheFolder
            this.devGuildId = devGuildId
            this.prefix = prefix
        }

        lavaplayer.configuration.let {
            it.resamplingQuality = resamplingQuality
            it.opusEncodingQuality = encodingQuality
        }
    }
}