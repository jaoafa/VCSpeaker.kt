package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.EntitySnapshot
import com.jaoafa.vcspeaker.database.SnappableEntity
import com.jaoafa.vcspeaker.database.SnapshotFactory
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

object SpeechCacheTable : IntIdTable("speech_cache") {
    val providerId = varchar("provider_id", 255)
    val hash = varchar("hash", 32)
        .uniqueIndex("idx_speech_cache_hash")
    val lastUsedAt = timestamp("last_used_at")
}

class SpeechCacheEntity(id: EntityID<Int>) : IntEntity(id), SnappableEntity<SpeechCacheSnapshot, SpeechCacheEntity> {
    companion object : IntEntityClass<SpeechCacheEntity>(SpeechCacheTable)

    var providerId by SpeechCacheTable.providerId
    var hash by SpeechCacheTable.hash
    var lastUsedAt by SpeechCacheTable.lastUsedAt

    override fun fetchSnapshot() = transaction { SpeechCacheSnapshot.from(readValues) }
}

@Serializable
data class SpeechCacheSnapshot(
    val id: Int,
    val providerId: String,
    val hash: String,
    @Contextual val lastUsedAt: Instant,
) : EntitySnapshot<SpeechCacheEntity>() {
    companion object : SnapshotFactory<SpeechCacheSnapshot> {
        override fun from(row: ResultRow) = SpeechCacheSnapshot(
            id = row[SpeechCacheTable.id].value,
            providerId = row[SpeechCacheTable.providerId],
            hash = row[SpeechCacheTable.hash],
            lastUsedAt = row[SpeechCacheTable.lastUsedAt],
        )
    }

    override fun fetchEntity() = transaction {
        SpeechCacheEntity[this@SpeechCacheSnapshot.id]
    }
}
