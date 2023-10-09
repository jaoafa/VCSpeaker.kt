package com.jaoafa.vcspeaker.models.original

import dev.kord.common.entity.Snowflake

data class DiscordInvite(
    val code: String,
    val guildId: Snowflake,
    val guildName: String,
    val channelId: Snowflake,
    val channelName: String,
    val inviterId: Snowflake?,
    val inviterName: String?,
    val eventId: Snowflake?,
    val eventName: String?,
)
