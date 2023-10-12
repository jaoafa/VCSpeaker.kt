package com.jaoafa.vcspeaker.models.response.twitter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitterOEmbedResponse(
    val url: String,
    @SerialName("author_name")
    val authorName: String,
    @SerialName("author_url")
    val authorUrl: String,
    val html: String,
    val width: Long,
    val height: Long? = null,
    val type: String,
    @SerialName("cache_age")
    val cacheAge: String,
    @SerialName("provider_name")
    val providerName: String,
    @SerialName("provider_url")
    val providerUrl: String,
    val version: String,
)
