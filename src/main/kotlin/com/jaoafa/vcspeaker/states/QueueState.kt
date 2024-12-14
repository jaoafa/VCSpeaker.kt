package com.jaoafa.vcspeaker.states

import com.jaoafa.vcspeaker.tts.Entry
import dev.kord.common.entity.Snowflake

class QueueState : StateStruct<Map<Snowflake, List<Entry>>>(mapOf()) {
    fun add(entry: Entry) {
        modify { state ->
            val newState = state.toMutableMap()
            val guildId = entry.guild.id
            newState[guildId] = newState[guildId]?.plus(entry) ?: listOf(entry)
            newState
        }
    }

    fun removeFirstOf(guildId: Snowflake): Entry? {
        val queue = get()[guildId] ?: return null
        val first = queue.firstOrNull() ?: return null

        modify { state ->
            val newState = state.toMutableMap()
            newState[guildId] = queue.drop(1)
            newState
        }

        return first
    }
}