package com.jaoafa.vcspeaker.api.update.types

import kotlinx.serialization.Serializable

@Serializable
data class UpdateError(
    val message: String
)
