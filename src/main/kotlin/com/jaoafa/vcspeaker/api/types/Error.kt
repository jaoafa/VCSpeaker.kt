package com.jaoafa.vcspeaker.api.types

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val message: String
)
