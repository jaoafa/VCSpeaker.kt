package com.jaoafa.vcspeaker.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

open class SnowflakeIdTable(name: String = "", columnName: String = "did") : IdTable<Snowflake>(name) {
    override val id: Column<EntityID<Snowflake>> = long(columnName).transform(SnowflakeTransformer()).entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

open class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

open class SnowflakeEntityClass<out E : SnowflakeEntity>(
    table: IdTable<Snowflake>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Snowflake>) -> E)? = null
) : EntityClass<Snowflake, E>(table, entityType, entityCtor)