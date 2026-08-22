package com.jaoafa.vcspeaker.models.response.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * HTTP 429 (Too Many Requests) レスポンスボディの構造。
 * Discord API は `Retry-After` ヘッダーとともに JSON ボディに `retry_after` フィールドを返す場合がある。
 * 両者が存在する場合、ヘッダーを優先するが、ヘッダーが無い場合のフォールバックとしてボディを参照する。
 */
@Serializable
internal data class DiscordRateLimitResponse(
    @SerialName("retry_after")
    val retryAfter: Double? = null
)
