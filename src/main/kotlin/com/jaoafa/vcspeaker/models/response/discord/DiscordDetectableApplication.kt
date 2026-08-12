package com.jaoafa.vcspeaker.models.response.discord

import kotlinx.serialization.Serializable

/**
 * `applications/detectable` レスポンス配列の要素。
 * 他のフィールドは ignoreUnknownKeys = true で無視する。
 */
@Serializable
internal data class DiscordDetectableApplication(
    val id: String,
    val name: String
)
