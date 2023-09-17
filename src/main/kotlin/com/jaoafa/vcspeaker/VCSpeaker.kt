package com.jaoafa.vcspeaker

import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tools.Discord.asChannelOf
import com.jaoafa.vcspeaker.tools.Discord.respond
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
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.TextChannel
import dev.kord.voice.AudioFrame
import java.io.File

object VCSpeaker {
    lateinit var instance: ExtensibleBot
    lateinit var kord: Kord
    lateinit var voiceText: VoiceTextAPI
    lateinit var config: Config

    var cachePolicy: Int = 7

    var dev: Snowflake? = null
    fun isDev() = dev != null

    val lavaplayer = DefaultAudioPlayerManager()

    // fixme Narrator already has guildId
    val narrators = hashMapOf<Snowflake, Narrator>()

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.join(interaction: PublicInteractionContext? = null): Narrator {
        narrators.remove(guild.id) // force disconnection

        val player = lavaplayer.createPlayer()

        val connection = (this as VoiceChannelBehavior).connect {
            audioProvider {
                AudioFrame.fromData(player.provide()?.data ?: ByteArray(0))
            }
        }

        val narrator = Narrator(guild.id, player, connection)
        narrators[guild.id] = narrator

        narrator.announce(
            NarrationScripts.SELF_JOIN,
            "**:loudspeaker: $mention に接続しました。**",
            interaction
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.move(interaction: PublicInteractionContext? = null): Narrator? {
        val narrator = narrators[guild.id] ?: return null

        narrator.connection.move(id)

        narrator.announce(
            NarrationScripts.SELF_MOVE,
            "**:loudspeaker: $mention に移動しました。**",
            interaction
        )

        return narrator
    }

    @OptIn(KordVoice::class)
    suspend fun BaseVoiceChannelBehavior.leave(interaction: PublicInteractionContext? = null) {
        val narrator = narrators[guild.id] ?: return
        narrator.connection.leave()
        narrator.player.destroy()
        narrators.remove(guild.id)

        if (interaction != null) {
            interaction.respond("**:wave: $mention から退出しました。**")
        } else {
            val channel = GuildStore.getOrDefault(guildId).channelId?.asChannelOf<TextChannel>()

            channel?.createMessage("**:wave: $mention から退出しました。**")
        }
    }

    object Files {
        private operator fun File.plus(file: File) = File(this, file.name)

        val storeFolder = File("stores")
        val cacheFolder = File("cache")
        val caches = storeFolder + File("caches.json")
        val guilds = storeFolder + File("guilds.json")
        val ignores = storeFolder + File("ignores.json")
        val aliases = storeFolder + File("aliases.json")
        val voices = storeFolder + File("voices.json")
        val titles = storeFolder + File("titles.json")
    }

    init {
        AudioSourceManagers.registerLocalSource(lavaplayer)

        val storeFolder = Files.storeFolder
        if (!storeFolder.exists()) storeFolder.mkdir()

        val cacheFolder = Files.cacheFolder
        if (!cacheFolder.exists()) cacheFolder.mkdir()
    }
}