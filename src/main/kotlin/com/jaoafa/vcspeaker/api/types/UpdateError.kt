package com.jaoafa.vcspeaker.api.types

import kotlinx.serialization.Serializable

@Serializable
data class UpdateError(
    val message: String
)
