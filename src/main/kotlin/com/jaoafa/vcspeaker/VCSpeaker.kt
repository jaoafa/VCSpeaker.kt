package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.configs.EnvSpec
import com.jaoafa.vcspeaker.tools.Emoji
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kordex.core.ExtensibleBot
import java.io.File
import kotlin.io.path.Path

object VCSpeaker {
    lateinit var version: String

    lateinit var args: Array<String>

    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord
    val lavaplayer = DefaultAudioPlayerManager()

    lateinit var config: Config
    lateinit var options: Options

    lateinit var storeFolder: File
    lateinit var cacheFolder: File

    lateinit var prefix: String

    var apiServer: Server? = null

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
     * @param config 設定ファイルから読み込んだ [Config] オブジェクト
     * @param options CLI 引数から読み込んだ [Options] オブジェクト
     */
    fun init(
        version: String,
        config: Config,
        options: Options,
    ) {
        VCSpeaker.run {
            this.version = version
            this.config = config
            this.options = options
            this.storeFolder = (options.storePath ?: Path(config[EnvSpec.storeFolder])).toFile()
            this.cacheFolder = (options.cachePath ?: Path(config[EnvSpec.cacheFolder])).toFile()
            this.devGuildId = (options.devGuildId ?: config[EnvSpec.devGuildId])?.let { Snowflake(it) }
            this.prefix = options.prefix ?: config[EnvSpec.commandPrefix]
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

    fun removeShutdownHook() = Runtime.getRuntime().removeShutdownHook(instance.shutdownHook)
}