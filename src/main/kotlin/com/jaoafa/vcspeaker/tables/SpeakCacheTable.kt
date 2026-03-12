package com.jaoafa.vcspeaker.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object SpeakCacheTable : IntIdTable("speak_cache") {
    val providerType = varchar("provider_type", 255)
    val hash = varchar("hash", 32)
        .uniqueIndex("idx_speak_cache_hash")
    val lastUsedAt = timestamp("last_used_at")
}

class SpeakCacheEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpeakCacheEntity>(SpeakCacheTable)

    var providerType by SpeakCacheTable.providerType
    var hash by SpeakCacheTable.hash
    var lastUsedAt by SpeakCacheTable.lastUsedAt
}