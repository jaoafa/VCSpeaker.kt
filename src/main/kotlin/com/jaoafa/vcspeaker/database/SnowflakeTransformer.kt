package com.jaoafa.vcspeaker.database

import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toLong
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.toSnowflake
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class SnowflakeTransformer : ColumnTransformer<Long, Snowflake> {
    override fun unwrap(value: Snowflake) = value.toLong()
    override fun wrap(value: Long) = value.toSnowflake()
}

class NullableSnowflakeTransformer : ColumnTransformer<Long?, Snowflake?> {
    override fun unwrap(value: Snowflake?) = value?.toLong()
    override fun wrap(value: Long?) = value?.toSnowflake()
}

class EntitySnowflakeTransformer : ColumnTransformer<EntityID<Long>, Snowflake> {
    override fun unwrap(value: Snowflake) = EntityID(value.toLong(), GuildTable)
    override fun wrap(value: EntityID<Long>) = value.value.toSnowflake()
}