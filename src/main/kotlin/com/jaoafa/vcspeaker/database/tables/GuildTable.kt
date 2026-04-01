package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object GuildTable : SnowflakeIdTable("guild", columnName = "did"), VersionedTable {
    val channelDid = long("channel_did").nullable()
        .transform(NullableSnowflakeTransformer())
    val prefix = varchar("prefix", 16).nullable()
    val autoJoin = bool("auto_join").default(false)
    val speakerVoiceId = reference(
        "speaker_voice_id",
        VoiceTable,
        fkName = "fk_guild_speaker_voice"
    )
    override val version = version()
}

class GuildEntity(id: EntityID<Snowflake>) : SnowflakeEntity(id), SnappableEntity<GuildSnapshot, GuildEntity> {
    companion object : SnowflakeEntityClass<GuildEntity>(GuildTable)

    var channelDid by GuildTable.channelDid
    var prefix by GuildTable.prefix
    var autoJoin by GuildTable.autoJoin
    var speakerVoiceEntity by VoiceEntity referencedOn GuildTable.speakerVoiceId

    override fun getSnapshot() = transaction { GuildSnapshot.from(readValues) }
}

@Serializable
data class GuildSnapshot(
    val did: Snowflake,
    val channelDid: Snowflake?,
    val prefix: String?,
    val autoJoin: Boolean,
    val speakerVoiceId: Int,
    val version: Int,
) : EntitySnapshot<GuildEntity>() {
    companion object : SnapshotFactory<GuildSnapshot> {
        override fun from(row: ResultRow) = GuildSnapshot(
            did = row[GuildTable.id].value,
            channelDid = row[GuildTable.channelDid],
            prefix = row[GuildTable.prefix],
            autoJoin = row[GuildTable.autoJoin],
            speakerVoiceId = row[GuildTable.speakerVoiceId].value,
            version = row[GuildTable.version],
        )
    }

    override fun fetchEntity() = transaction {
        GuildEntity[this@GuildSnapshot.did]
    }
}
