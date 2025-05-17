package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.reload.state.StateEntry
import com.jaoafa.vcspeaker.tts.Speech
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class NarratorState(
    val guildId: Snowflake,
    val channelId: Snowflake,
    val queue: List<Speech>
): StateEntry
