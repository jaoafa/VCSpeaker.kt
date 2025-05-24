package com.jaoafa.vcspeaker.reload

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateRequest<T>(
    val sequence: Int = 0,
    val data: T
)