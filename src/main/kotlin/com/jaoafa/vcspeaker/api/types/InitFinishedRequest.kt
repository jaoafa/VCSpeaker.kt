package com.jaoafa.vcspeaker.api.types

import kotlinx.serialization.Serializable

@Serializable
data class InitFinishedRequest(
    val id: String,
    val token: String
)