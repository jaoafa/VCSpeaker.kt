package com.jaoafa.vcspeaker.tts.narrators

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior

object Narrators {
    val list = mutableListOf<Narrator>()

    operator fun get(guildId: Snowflake) = list.find { it.guildId == guildId }

    fun GuildBehavior.narrator() = get(id)

    operator fun plusAssign(narrator: Narrator) {
        this -= narrator.guildId
        list.add(narrator)
    }

    operator fun minusAssign(guildId: Snowflake) {
        list.removeIf { it.guildId == guildId }
    }
}