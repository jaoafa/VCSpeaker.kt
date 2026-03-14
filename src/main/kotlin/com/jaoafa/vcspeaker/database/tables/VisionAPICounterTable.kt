package com.jaoafa.vcspeaker.database.tables

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

class VisionAPICounterEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<VisionAPICounterEntity>(VisionAPICounterTable)

    var count by VisionAPICounterTable.count
    var limitReachedAt by VisionAPICounterTable.limitReachedAt
}