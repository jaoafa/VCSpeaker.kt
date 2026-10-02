package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.tables.DEFAULT_SOUNDBOARD_VOLUME
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object GuildAction {
    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は null を返します。
     */
    fun GuildBehavior.getEntityOrNull() = transaction {
        GuildEntity.findById(this@getEntityOrNull.id)
    }

    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は [IllegalStateException] をスローします。
     * check { anyGuildRegistered() } でチェックされた後に使用されることを想定しています。
     */
    fun GuildBehavior.getEntity() =
        getEntityOrNull() ?: throw IllegalStateException("Guild ${id.value} is not registered.")

    fun GuildBehavior.getSnapshot() = getEntity().getSnapshot()

    suspend fun GuildBehavior.getVoiceTextChannelOrNull(): TextChannel? {
        val channelId = transaction { getEntity().channelDid } ?: return null

        return getChannelOf<TextChannel>(channelId)
    }

    suspend fun getSoundboardVolume(guildId: Snowflake): Int =
        suspendTransaction { GuildEntity.findById(guildId)?.soundboardVolume } ?: DEFAULT_SOUNDBOARD_VOLUME

    suspend fun GuildBehavior.getSoundboardVolume(): Int = getSoundboardVolume(id)

    fun GuildBehavior.isAutoJoinEnabled(): Boolean = transaction { getEntity().autoJoin }

    fun GuildBehavior.getVoice(): Voice {
        val snapshot = transaction { getEntity().speakerVoiceEntity.getSnapshot() }

        return Voice.from(snapshot)
    }
}
