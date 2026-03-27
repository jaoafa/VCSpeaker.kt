package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.TypedEntity
import com.jaoafa.vcspeaker.database.TypedRow
import com.jaoafa.vcspeaker.database.toTyped
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object SpeechCacheTable : IntIdTable("speech_cache") {
    val providerId = varchar("provider_id", 255)
    val hash = varchar("hash", 32)
        .uniqueIndex("idx_speech_cache_hash")
    val lastUsedAt = timestamp("last_used_at")
}

class SpeechCacheEntity(id: EntityID<Int>) : IntEntity(id), TypedEntity<SpeechCacheRow> {
    companion object : IntEntityClass<SpeechCacheEntity>(SpeechCacheTable)

    var providerId by SpeechCacheTable.providerId
    var hash by SpeechCacheTable.hash
    var lastUsedAt by SpeechCacheTable.lastUsedAt

    override fun getRow() = readValues.toTyped<SpeechCacheRow>()
}

class SpeechCacheRow(val resultRow: ResultRow) : TypedRow(resultRow, SpeechCacheTable) {
    val id = column(SpeechCacheTable.id)
    val providerId = column(SpeechCacheTable.providerId)
    val hash = column(SpeechCacheTable.hash)
    val lastUsedAt = column(SpeechCacheTable.lastUsedAt)

    override fun describe() = resultRow.toString()
}