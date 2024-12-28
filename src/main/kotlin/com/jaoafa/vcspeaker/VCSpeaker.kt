package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.tools.Emoji
import com.jaoafa.vcspeaker.tts.api.VoiceTextAPI
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kordex.core.ExtensibleBot
import java.io.File
import java.util.*
import kotlin.io.path.Path
import kotlin.properties.Delegates

object VCSpeaker {
    val uuid = UUID.randomUUID()

    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord
    val lavaplayer = DefaultAudioPlayerManager()

    lateinit var voicetext: VoiceTextAPI
    lateinit var config: Config

    lateinit var storeFolder: File
    lateinit var cacheFolder: File

    lateinit var prefix: String

    // 開発環境のコマンドを登録する Guild ID (null で開発環境を無効化)
    var devGuildId: Snowflake? = null

    var port by Delegates.notNull<Int>()
    var autoUpdate by Delegates.notNull<Boolean>()

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
     * @param voicetext [VoiceTextAPI] インスタンス
     * @param config [Config]
     */
    fun init(
        config: Config,
        options: Options,
        voicetext: VoiceTextAPI,
    ) {
        VCSpeaker.run {
            this.voicetext = voicetext
            this.config = config
            this.storeFolder = (options.storePath ?: Path(config[EnvSpec.storeFolder])).toFile()
            this.cacheFolder = (options.cachePath ?: Path(config[EnvSpec.cacheFolder])).toFile()
            this.devGuildId = (options.devGuildId ?: config[EnvSpec.devGuildId])?.let { Snowflake(it) }
            this.prefix = options.prefix ?: config[EnvSpec.commandPrefix]
            this.autoUpdate = options.autoUpdate ?: config[EnvSpec.autoUpdate]
            this.port = options.port ?: config[EnvSpec.port]
        }

        Emoji // init

        AudioSourceManagers.registerLocalSource(lavaplayer)

        if (!storeFolder.exists()) storeFolder.mkdir()
        if (!cacheFolder.exists()) cacheFolder.mkdir()

        lavaplayer.configuration.let {
            it.resamplingQuality = options.resamplingQuality ?: config[EnvSpec.resamplingQuality]
            it.opusEncodingQuality = options.encodingQuality ?: config[EnvSpec.encodingQuality]
        }
    }
}