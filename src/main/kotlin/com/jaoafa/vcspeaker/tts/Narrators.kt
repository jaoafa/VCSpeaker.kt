package com.jaoafa.vcspeaker.tts

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior

object Narrators {
    private val narrators = mutableListOf<Narrator>()

    operator fun get(guildId: Snowflake) = narrators.find { it.guildId == guildId }

    fun GuildBehavior.narrator() = get(id)

    operator fun plusAssign(narrator: Narrator) {
        this -= narrator.guildId
        narrators.add(narrator)
    }

    operator fun minusAssign(guildId: Snowflake) {
        narrators.removeIf { it.guildId == guildId }
    }
}