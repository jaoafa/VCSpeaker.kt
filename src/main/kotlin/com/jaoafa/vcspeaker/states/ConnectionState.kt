package com.jaoafa.vcspeaker.states

import dev.kord.common.entity.Snowflake

class ConnectionState : StateStruct<Map<Snowflake, Snowflake>>(mapOf()) {
    fun set(guildId: Snowflake, channelId: Snowflake) {
        modify { state ->
            val newState = state.toMutableMap()
            newState[guildId] = channelId
            newState
        }
    }

    fun remove(guildId: Snowflake) {
        modify { state ->
            state.filter { it.key != guildId }
        }
    }
}