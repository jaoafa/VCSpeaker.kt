package com.jaoafa.vcspeaker.models.response.steam

import kotlinx.serialization.Serializable

@Serializable
data class SteamAppDetail(
    val success: Boolean,
    val data: SteamAppDetailData?
)

@Serializable
data class SteamAppDetailData(
    val type: String,
    val name: String
)