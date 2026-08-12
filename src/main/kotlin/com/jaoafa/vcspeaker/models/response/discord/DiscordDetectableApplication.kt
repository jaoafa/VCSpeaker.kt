package com.jaoafa.vcspeaker.models.response.discord

import kotlinx.serialization.Serializable

/**
 * `applications/detectable` レスポンス配列の要素。
 * 実際のレスポンスは他に多数のフィールド(aliases 等)を持つが、
 * このアプリケーションでは id / name のみを使用するため、
 * ContentNegotiation の ignoreUnknownKeys = true で残りを無視する。
 */
@Serializable
internal data class DiscordDetectableApplication(
    val id: String,
    val name: String
)
