package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.*
import com.jaoafa.vcspeaker.database.DatabaseUtil.version
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID

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

class GuildEntity(id: EntityID<Snowflake>) : SnowflakeEntity(id), TypedEntity<GuildRow> {
    companion object : SnowflakeEntityClass<GuildEntity>(GuildTable)

    var channelDid by GuildTable.channelDid
    var prefix by GuildTable.prefix
    var autoJoin by GuildTable.autoJoin
    var speakerVoiceEntity by VoiceEntity referencedOn GuildTable.speakerVoiceId

    override fun getRow() = readValues.toTyped<GuildRow>()
}

class GuildRow(val resultRow: ResultRow) : TypedRow(resultRow, GuildTable) {
    val did = column(GuildTable.id)
    val channelDid = column(GuildTable.channelDid)
    val prefix = column(GuildTable.prefix)
    val autoJoin = column(GuildTable.autoJoin)
    val speakerVoiceId = column(GuildTable.speakerVoiceId)
    val version = column(GuildTable.version)

    override fun describe() = resultRow.toString()
}