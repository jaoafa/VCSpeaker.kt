package com.jaoafa.vcspeaker.models.response.youtube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeOEmbedResponse(
    @SerialName("author_name")
    val authorName: String,
    @SerialName("author_url")
    val authorUrl: String,
    val height: Int,
    val html: String,
    @SerialName("provider_name")
    val providerName: String,
    @SerialName("provider_url")
    val providerUrl: String,
    @SerialName("thumbnail_height")
    val thumbnailHeight: Int,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    @SerialName("thumbnail_width")
    val thumbnailWidth: Int,
    val title: String,
    val type: String,
    val version: String,
    val width: Int
)
