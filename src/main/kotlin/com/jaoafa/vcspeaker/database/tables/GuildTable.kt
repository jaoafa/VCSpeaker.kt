package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.tools.DatabaseUtil.transformSnowflakeNullable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object GuildTable : LongIdTable("guild", columnName = "did") {
    val channelDid = long("channel_did").nullable()
    val prefix = varchar("prefix", 16).nullable()
    val autoJoin = bool("auto_join").default(false)
    val speakerVoiceId = reference(
        "speaker_voice_id",
        VoiceTable,
        fkName = "fk_guild_speaker_voice"
    )
}

class GuildEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GuildEntity>(GuildTable)

    var channelDid by GuildTable.channelDid.transformSnowflakeNullable()
    var prefix by GuildTable.prefix
    var autoJoin by GuildTable.autoJoin
    var speakerVoiceEntity by VoiceEntity referencedOn GuildTable.speakerVoiceId
}