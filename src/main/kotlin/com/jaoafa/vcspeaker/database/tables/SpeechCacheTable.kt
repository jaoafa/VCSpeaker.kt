package com.jaoafa.vcspeaker.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object SpeechCacheTable : IntIdTable("speech_cache") {
    val providerType = varchar("provider_type", 255)
    val hash = varchar("hash", 32)
        .uniqueIndex("idx_speech_cache_hash")
    val lastUsedAt = timestamp("last_used_at")
}

class SpeechCacheEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpeechCacheEntity>(SpeechCacheTable)

    var providerType by SpeechCacheTable.providerType
    var hash by SpeechCacheTable.hash
    var lastUsedAt by SpeechCacheTable.lastUsedAt
}