package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.actions.GuildAction.getEntity
import com.jaoafa.vcspeaker.database.suspendTransactionResulting
import com.jaoafa.vcspeaker.database.tables.VCTitleSnapshot
import com.jaoafa.vcspeaker.database.transactionResulting
import com.jaoafa.vcspeaker.database.unwrap
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.getName
import com.jaoafa.vcspeaker.tools.discord.VoiceExtensions.rename
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.jaoafa.vcspeaker.database.tables.VCTitleEntity as Entity
import com.jaoafa.vcspeaker.database.tables.VCTitleTable as Table

object TitleAction {
    private val logger = KotlinLogging.logger { }

    fun getTitleEntityOf(channel: BaseVoiceChannelBehavior) = transaction {
        Entity.find { Table.channelDid eq channel.id }.singleOrNull()
    }

    suspend fun setTitleOf(
        channel: BaseVoiceChannelBehavior,
        title: String,
        creator: UserBehavior
    ): Pair<VCTitleSnapshot?, VCTitleSnapshot> = suspendTransaction transaction@{
        val entity = getTitleEntityOf(channel)
        val oldSnapshot = entity?.getSnapshot()

        val originalName = channel.getName()

        channel.rename(title)

        val newEntity = transactionResulting(commit = true) {
            if (entity != null) {
                entity.title = title
                entity.creatorDid = creator.id
                entity.version += 1
                entity
            } else {
                Entity.new {
                    this.title = title
                    this.channelDid = channel.id
                    this.guildEntity = channel.guild.getEntity()
                    this.creatorDid = creator.id
                    this.originalTitle = originalName
                }
            }
        }.unwrap()

        val newSnapshot = newEntity.getSnapshot()

        logger.info { "Title Set: $oldSnapshot -> $newSnapshot" }

        return@transaction oldSnapshot to newSnapshot
    }

    /**
     * [channel] に設定されたタイトルをリセットします。
     *
     * @param channel 対象のボイスチャンネル
     * @param creator 操作の実行者
     * @return リセットが行われなかった場合は null, リセットが行われた場合は操作前後のレコードを返します。
     */
    suspend fun resetTitleOf(
        channel: BaseVoiceChannelBehavior,
        creator: UserBehavior
    ): Pair<VCTitleSnapshot?, VCTitleSnapshot>? =
        suspendTransaction transaction@{
            val entity = getTitleEntityOf(channel)
            val oldSnapshot = entity?.getSnapshot()

            if (entity == null || oldSnapshot?.title == null) {
                return@transaction null
            }

            channel.rename(oldSnapshot.originalTitle)

            transactionResulting(commit = true) {
                entity.title = null
                entity.creatorDid = creator.id
                entity.version += 1
            }.unwrap()

            val newSnapshot = entity.getSnapshot()

            logger.info { "Title Reset: $oldSnapshot -> $newSnapshot" }

            return@transaction oldSnapshot to newSnapshot

        }

    /**
     * 現在のボイスチャットのチャンネル名を、元のタイトルとして保存します。
     *
     * @param channel 対象のボイスチャンネル
     * @param creator 操作の実行者
     * @return レコードが存在しない場合は null, 保存が行われた場合は操作前後のレコードを返します。
     */
    suspend fun saveTitleOf(
        channel: BaseVoiceChannelBehavior,
        creator: UserBehavior
    ): Pair<VCTitleSnapshot, VCTitleSnapshot>? =
        suspendTransaction transaction@{
            val entity = getTitleEntityOf(channel) ?: return@transaction null
            val oldSnapshot = entity.getSnapshot()

            suspendTransactionResulting(commit = true) {
                entity.originalTitle = channel.getName()
                entity.title = null
                entity.creatorDid = creator.id
                entity.version += 1
            }.unwrap()

            val newSnapshot = entity.getSnapshot()

            logger.info { "Title Saved: $oldSnapshot -> $newSnapshot" }

            return@transaction oldSnapshot to newSnapshot
        }

    suspend fun saveAllTitlesOf(guild: GuildBehavior, creator: UserBehavior): Map<VCTitleSnapshot, VCTitleSnapshot> =
        suspendTransaction transaction@{
            val entities = Entity.find { Table.guildDid eq guild.id }.toList()
            val oldSnapshots = entities.map { it.getSnapshot() }

            suspendTransactionResulting(commit = true) {
                for (entity in entities) {
                    val channel = guild.getChannel(entity.channelDid)

                    entity.originalTitle = channel.name
                    entity.title = null
                    entity.creatorDid = creator.id
                    entity.version += 1
                }
            }.unwrap()

            val newSnapshots = entities.map { it.getSnapshot() }

            return@transaction oldSnapshots.zip(newSnapshots).toMap()
        }
}
