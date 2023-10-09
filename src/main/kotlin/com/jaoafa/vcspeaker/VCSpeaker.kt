package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.voicetext.NarrationScripts
import com.jaoafa.vcspeaker.voicetext.Narrator
import com.jaoafa.vcspeaker.voicetext.NarratorExtensions.announce
import com.jaoafa.vcspeaker.voicetext.api.VoiceTextAPI
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.uchuhimo.konf.Config
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.Message
import dev.kord.voice.AudioFrame
import java.io.File
import java.util.concurrent.TimeUnit

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

    // fixme Narrator already has guildId
    val narrators = hashMapOf<Snowflake, Narrator>()

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.join(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
    ): Narrator {
        narrators.remove(guild.id) // force disconnection

        val player = lavaplayer.createPlayer()

        val connection = connect {
            audioProvider {
                AudioFrame.fromData(player.provide(1, TimeUnit.SECONDS)?.data)
            }
        }

        val narrator = Narrator(guild.id, player, connection)
        narrators[guild.id] = narrator

        narrator.announce(
            NarrationScripts.SELF_JOIN,
            "**:loudspeaker: $mention に接続しました。**",
            interaction,
            message
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.move(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
    ): Narrator? {
        val narrator = narrators[guild.id] ?: return null

        narrator.connection.move(id)

        narrator.announce(
            NarrationScripts.SELF_MOVE,
            "**:loudspeaker: $mention に移動しました。**",
            interaction,
            message
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.leave(
        interaction: PublicInteractionContext? = null,
        message: Message? = null
    ) {
        val narrator = narrators[guild.id] ?: return
        narrator.connection.leave()
        narrator.player.destroy()
        narrators.remove(guild.id)

        narrator.announce(
            "",
            "**:wave: $mention から退出しました。**",
            interaction,
            message
        )
    }

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