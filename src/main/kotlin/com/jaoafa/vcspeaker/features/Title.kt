package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntity
import com.jaoafa.vcspeaker.database.commitingSuspendTransaction
import com.jaoafa.vcspeaker.database.committingTransaction
import com.jaoafa.vcspeaker.database.tables.VCTitleRow
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

object Title {
    val logger = KotlinLogging.logger {}

    fun getTitleEntityOf(channel: BaseVoiceChannelBehavior) = transaction {
        Entity.find { Table.channelDid eq channel.id }.singleOrNull()
    }

    suspend fun setTitleOf(
        channel: BaseVoiceChannelBehavior,
        title: String,
        creator: UserBehavior
    ): Pair<VCTitleRow?, VCTitleRow> = suspendTransaction transaction@{
        val entity = getTitleEntityOf(channel)
        val oldRow = entity?.getRow()

        val originalName = channel.getName()

        channel.rename(title)

        val newEntity = committingTransaction {
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
        }

        val newRow = newEntity.getRow()

        logger.info { "Title Set: $oldRow -> $newRow" }

        return@transaction oldRow to newRow
    }

    /**
     * [channel] に設定されたタイトルをリセットします。
     *
     * @param channel 対象のボイスチャンネル
     * @param creator 操作の実行者
     * @return リセットが行われなかった場合は null, リセットが行われた場合は操作前後のレコードを返します。
     */
    suspend fun resetTitleOf(channel: BaseVoiceChannelBehavior, creator: UserBehavior): Pair<VCTitleRow?, VCTitleRow>? =
        suspendTransaction transaction@{
            val entity = getTitleEntityOf(channel)
            val oldRow = entity?.getRow()

            if (entity == null || oldRow?.title == null) {
                return@transaction null
            }

            channel.rename(oldRow.originalTitle)

            committingTransaction {
                entity.title = null
                entity.creatorDid = creator.id
                entity.version += 1
            }

            val newRow = entity.getRow()

            logger.info { "Title Reset: $oldRow -> $newRow" }

            return@transaction oldRow to newRow

        }

    /**
     * 現在のボイスチャットのチャンネル名を、元のタイトルとして保存します。
     *
     * @param channel 対象のボイスチャンネル
     * @param creator 操作の実行者
     * @return レコードが存在しない場合は null, 保存が行われた場合は操作前後のレコードを返します。
     */
    suspend fun saveTitleOf(channel: BaseVoiceChannelBehavior, creator: UserBehavior): Pair<VCTitleRow, VCTitleRow>? =
        suspendTransaction transaction@{
            val entity = getTitleEntityOf(channel) ?: return@transaction null
            val oldRow = entity.getRow()

            commitingSuspendTransaction {
                entity.originalTitle = channel.getName()
                entity.title = null
                entity.creatorDid = creator.id
                entity.version += 1
            }

            val newRow = entity.getRow()

            logger.info { "Title Saved: $oldRow -> $newRow" }

            return@transaction oldRow to newRow
        }

    suspend fun saveAllTitlesOf(guild: GuildBehavior, creator: UserBehavior): Map<VCTitleRow, VCTitleRow> =
        suspendTransaction transaction@{
            val entities = Entity.find { Table.guildDid eq guild.id }.toList()
            val oldRows = entities.map { it.getRow() }

            commitingSuspendTransaction {
                for (entity in entities) {
                    val channel = guild.getChannel(entity.channelDid)

                    entity.originalTitle = channel.name
                    entity.title = null
                    entity.creatorDid = creator.id
                    entity.version += 1
                }
            }

            val newRows = entities.map { it.getRow() }

            return@transaction oldRows.zip(newRows).toMap()
        }
}