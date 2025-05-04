package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.voice.AudioFrame
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit

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
    fun prepareAdd(guildId: Snowflake, channel: BaseVoiceChannelBehavior): suspend () -> Narrator {
        remove(guildId)

        val player = VCSpeaker.lavaplayer.createPlayer()

        logger.info { "Preparing Narrator for ${channel.id} at $guildId" }

        return {
            val connection = channel.connect {
                audioProvider {
                    AudioFrame.fromData(player.provide(1, TimeUnit.SECONDS)?.data)
                }
            }

            val narrator = Narrator(guildId = guildId, channelId = channel.id, player, connection)

            list.add(narrator)

            logger.info { "Connection Established: Narrator created for ${channel.id} at $guildId" }

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
            narrator.connection.leave()
            narrator.player.destroy()

            logger.info { "Connection destroyed for $guildId." }
        }
    }

    fun GuildBehavior.getNarrator() = get(id)
}