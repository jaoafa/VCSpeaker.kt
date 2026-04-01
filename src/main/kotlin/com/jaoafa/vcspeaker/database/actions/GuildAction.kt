package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object GuildAction {
    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は null を返します。
     */
    fun GuildBehavior.fetchEntityOrNull() = transaction {
        GuildEntity.findById(this@fetchEntityOrNull.id)
    }

    /**
     * GUILD テーブルに登録されている Guild のレコードを取得します。登録されていない場合は [IllegalStateException] をスローします。
     * check { anyGuildRegistered() } でチェックされた後に使用されることを想定しています。
     */
    fun GuildBehavior.fetchEntity() =
        fetchEntityOrNull() ?: throw IllegalStateException("Guild ${id.value} is not registered.")

    fun GuildBehavior.fetchSnapshot() = fetchEntity().fetchSnapshot()

    suspend fun GuildBehavior.getVoiceTextChannelOrNull(): TextChannel? {
        val channelId = transaction { fetchEntity().channelDid } ?: return null

        return getChannelOf<TextChannel>(channelId)
    }

    fun GuildBehavior.isAutoJoinEnabled(): Boolean = transaction { fetchEntity().autoJoin }

    fun GuildBehavior.getVoice(): Voice {
        val snapshot = transaction { fetchEntity().speakerVoiceEntity.fetchSnapshot() }

        return Voice.from(snapshot)
    }
}
