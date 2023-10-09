package com.jaoafa.vcspeaker.models.response.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordGetInviteResponse(
    val type: Long,
    val code: String,
    val inviter: Inviter,
    @SerialName("expires_at")
    val expiresAt: String,
    val flags: Long,
    val guild: Guild,
    @SerialName("guild_id")
    val guildId: String,
    val channel: Channel,
    @SerialName("guild_scheduled_event")
    val guildScheduledEvent: GuildScheduledEvent? = null,
    @SerialName("approximate_member_count")
    val approximateMemberCount: Long,
    @SerialName("approximate_presence_count")
    val approximatePresenceCount: Long,
)

@Serializable
data class AvatarDecorationData(
    val type: Long,
    val id: String,
)

@Serializable
data class Inviter(
    val id: String,
    val username: String,
    val avatar: String,
    val discriminator: String,
    @SerialName("public_flags")
    val publicFlags: Long,
    val flags: Long,
    val banner: String? = null,
    @SerialName("accent_color")
    val accentColor: Long? = null,
    @SerialName("global_name")
    val globalName: String,
    @SerialName("avatar_decoration_data")
    val avatarDecorationData: AvatarDecorationData? = null,
    @SerialName("banner_color")
    val bannerColor: Long? = null,
)

@Serializable
data class Guild(
    val id: String,
    val name: String,
    val splash: String,
    val banner: String,
    val description: String? = null,
    val icon: String,
    val features: List<String>,
    @SerialName("verification_level")
    val verificationLevel: Long,
    @SerialName("vanity_url_code")
    val vanityUrlCode: String? = null,
    @SerialName("nsfw_level")
    val nsfwLevel: Long,
    val nsfw: Boolean,
    @SerialName("premium_subscription_count")
    val premiumSubscriptionCount: Long,
)

@Serializable
data class Channel(
    val id: String,
    val type: Long,
    val name: String,
)

@Serializable
data class GuildScheduledEvent(
    val id: String,
    @SerialName("guild_id")
    val guildId: String,
    val name: String,
    val description: String,
    @SerialName("channel_id")
    val channelId: String? = null,
    @SerialName("creator_id")
    val creatorId: String,
    val image: String,
    @SerialName("scheduled_start_time")
    val scheduledStartTime: String,
    @SerialName("scheduled_end_time")
    val scheduledEndTime: String,
    val status: Long,
    @SerialName("entity_type")
    val entityType: Long,
    @SerialName("entity_id")
    val entityId: String? = null,
    @SerialName("recurrence_rule")
    val recurrenceRule: String? = null,
    @SerialName("user_count")
    val userCount: Long,
    @SerialName("privacy_level")
    val privacyLevel: Long,
    @SerialName("sku_ids")
    val skuIds: List<String>,
    @SerialName("auto_start")
    val autoStart: Boolean,
    @SerialName("entity_metadata")
    val entityMetadata: EntityMetadata,
)

@Serializable
data class EntityMetadata(
    val location: String,
)
