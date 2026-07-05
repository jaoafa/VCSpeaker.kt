package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.EntitySnapshot
import com.jaoafa.vcspeaker.database.SnappableEntity
import com.jaoafa.vcspeaker.database.SnapshotFactory
import com.jaoafa.vcspeaker.database.actions.VisionApiCounterAction.VISION_API_LIMIT
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

object VisionAPICounterTable : CompositeIdTable("vision_api_counter") {
    val year = integer("year").entityId()
    val month = integer("month").entityId()
    val count = integer("count").default(0)
    val limitReachedAt = timestamp("limit_reached_at").nullable()

    override val primaryKey = PrimaryKey(year, month)
}

class VisionAPICounterEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    SnappableEntity<VisionAPICounterSnapshot, VisionAPICounterEntity> {
    companion object : CompositeEntityClass<VisionAPICounterEntity>(VisionAPICounterTable)

    var count by VisionAPICounterTable.count
    var limitReachedAt by VisionAPICounterTable.limitReachedAt

    override fun getSnapshot() = transaction { VisionAPICounterSnapshot.from(readValues) }
}

@Serializable
data class VisionAPICounterSnapshot(
    val year: Int,
    val month: Int,
    val count: Int,
    @Contextual val limitReachedAt: Instant?,
) : EntitySnapshot<VisionAPICounterEntity>() {
    companion object : SnapshotFactory<VisionAPICounterSnapshot> {
        override fun from(row: ResultRow) = VisionAPICounterSnapshot(
            year = row[VisionAPICounterTable.year].value,
            month = row[VisionAPICounterTable.month].value,
            count = row[VisionAPICounterTable.count],
            limitReachedAt = row[VisionAPICounterTable.limitReachedAt],
        )
    }

    override fun describe() =
        "Vision API Counter $year/$month: $count/$VISION_API_LIMIT requests, ${if (limitReachedAt != null) "limit reached at $limitReachedAt" else "limit not reached"}"

    fun isLimitReached() = count >= VISION_API_LIMIT

    override fun getEntity() = transaction {
        VisionAPICounterEntity.findById(CompositeID {
            it[VisionAPICounterTable.year] = this@VisionAPICounterSnapshot.year
            it[VisionAPICounterTable.month] = this@VisionAPICounterSnapshot.month
        })
            ?: error("VisionAPICounterEntity not found: ${this@VisionAPICounterSnapshot.year}/${this@VisionAPICounterSnapshot.month}")
    }
}

