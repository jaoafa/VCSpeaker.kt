package com.jaoafa.vcspeaker.database.tables

import com.jaoafa.vcspeaker.database.TypedEntity
import com.jaoafa.vcspeaker.database.TypedRow
import com.jaoafa.vcspeaker.database.actions.VisionApiCounterAction.VISION_API_LIMIT
import com.jaoafa.vcspeaker.database.toTyped
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object VisionAPICounterTable : CompositeIdTable("vision_api_counter") {
    val year = integer("year").entityId()
    val month = integer("month").entityId()
    val count = integer("count").default(0)
    val limitReachedAt = timestamp("limit_reached_at").nullable()

    override val primaryKey = PrimaryKey(year, month)
}

class VisionAPICounterEntity(id: EntityID<CompositeID>) : CompositeEntity(id), TypedEntity<VisionAPICounterRow> {
    companion object : CompositeEntityClass<VisionAPICounterEntity>(VisionAPICounterTable)

    var count by VisionAPICounterTable.count
    var limitReachedAt by VisionAPICounterTable.limitReachedAt

    override fun getRow() = readValues.toTyped<VisionAPICounterRow>()
}

class VisionAPICounterRow(resultRow: ResultRow) : TypedRow(resultRow, VisionAPICounterTable) {
    val year = column(VisionAPICounterTable.year)
    val month = column(VisionAPICounterTable.month)
    val count = column(VisionAPICounterTable.count)
    val limitReachedAt = column(VisionAPICounterTable.limitReachedAt)

    override fun describe() =
        "Vision API Counter $year/$month: $count/$VISION_API_LIMIT requests, ${if (limitReachedAt != null) "limit reached at $limitReachedAt" else "limit not reached"}"

    fun isLimitReached() = count >= VISION_API_LIMIT
}
