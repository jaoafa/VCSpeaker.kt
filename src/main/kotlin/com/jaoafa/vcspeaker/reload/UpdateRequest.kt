package com.jaoafa.vcspeaker.reload

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRequest<T>(
    val sequence: Int = 0,
    val data: T
)