package com.jaoafa.vcspeaker.state

import com.jaoafa.vcspeaker.tts.Speech
import com.jaoafa.vcspeaker.tts.narrators.Narrators
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val queue: Map<Snowflake, List<Speech>>,
) {
    companion object {
        fun generate(): State {
            val queue = Narrators.list.associate { it.guildId to it.scheduler.queue }

            return State(queue)
        }
    }
}