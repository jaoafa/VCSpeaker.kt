package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.Scheduler
import com.jaoafa.vcspeaker.tts.Speech
import com.jaoafa.vcspeaker.tts.providers.BatchProvider
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.VoiceChannel
import dev.schlaubi.lavakord.kord.connectAudio
import dev.schlaubi.lavakord.kord.getLink
import io.github.oshai.kotlinlogging.KotlinLogging

object NarratorManager {
    private val logger = KotlinLogging.logger { }

    val list = mutableListOf<Narrator>()

    operator fun get(guildId: Snowflake) = list.find { it.guildId == guildId }

    /**
     * 指定したサーバーのボイスチャンネルに接続し、[Narrator] の登録の準備をします。
     *
     * @param guildId サーバー ID
     * @param channel ボイスチャンネル
     *
     * @return 登録と接続を実行する関数
     */
    @OptIn(KordVoice::class)
    fun prepareAdd(
        guildId: Snowflake,
        channelId: Snowflake,
        queue: List<Speech> = emptyList()
    ): suspend () -> Narrator {
        remove(guildId)

        logger.info { "Preparing Narrator for $channelId at $guildId" }

        return {
            val guild =
                VCSpeaker.kord.getGuildOrNull(guildId)
                    ?: throw IllegalStateException("Error while connecting; Guild not found: $guildId")
            val channel = guild.getChannelOfOrNull<VoiceChannel>(channelId)
                ?: throw IllegalStateException("Error while connecting; Channel not found: $channelId")

//            val connection = channel.connect {
//                audioProvider {
//                    AudioFrame.fromData(player.provide(1, TimeUnit.SECONDS)?.data)
//                }
//            }

            val link = VCSpeaker.lavalink.getLink(guild.id)
            link.connectAudio(channel.id)

            val narrator = Narrator(
                guildId = guildId,
                channelId = channelId,
                link,
                Scheduler(link, queue.map {
                    it.copy(tracks = BatchProvider(link, it.contexts).start())
                }.toMutableList())
            )

            list.add(narrator)

            logger.info { "Connection Established: Narrator created for $channelId at $guildId" }

            narrator
        }
    }

    /**
     * 指定したサーバーの [Narrator] を削除します。
     *
     * @param guildId サーバー ID
     *
     * @return 接続の切断を実行する関数。null の場合は何も削除されていません
     */
    @OptIn(KordVoice::class)
    fun remove(guildId: Snowflake): (suspend () -> Unit)? {
        val narrator = list.firstOrNull { it.guildId == guildId }

        if (narrator == null) return null

        list.remove(narrator)

        logger.info { "Narrator for $guildId removed." }

        return {
            narrator.link.destroy()

            logger.info { "Connection destroyed for $guildId." }
        }
    }

    fun GuildBehavior.getNarrator() = get(id)
}