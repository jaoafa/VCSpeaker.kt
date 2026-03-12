package com.jaoafa.vcspeaker.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object VCTitleTable : IntIdTable("vc_title") {
    val guildDid = reference(
        "guild_did", GuildTable,
        fkName = "fk_vc_title_guild",
        onDelete = ReferenceOption.CASCADE
    ).index("idx_vc_title_guild")
    val title = varchar("title", 255)
    val originalTitle = varchar("original_title", 255)
    val channelDid = long("channel_did")
        .uniqueIndex("idx_vc_title_channel")
    val creatorDid = long("creator_did")
}

class VCTitleEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VCTitleEntity>(VCTitleTable)

    var guildEntity by GuildEntity referencedOn VCTitleTable.guildDid
    var title by VCTitleTable.title
    var originalTitle by VCTitleTable.originalTitle
    var channelDid by VCTitleTable.channelDid
    var creatorDid by VCTitleTable.creatorDid
}