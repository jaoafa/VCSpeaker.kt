package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import dev.kord.common.entity.Snowflake

object Ignore {
    fun String.shouldIgnoreOn(guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            when (it.type) {
                IgnoreType.Equals -> this == it.search
                IgnoreType.Contains -> contains(it.search)
            }
        }
}