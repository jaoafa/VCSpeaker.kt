package com.jaoafa.vcspeaker.reload

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import kotlin.io.encoding.Base64

@Serializable
data class ReloadServerCredential(
    val id: String,
    val token: String
) {
    companion object {
        fun generate() = ReloadServerCredential(
            id = Reload.serverIds.random(),
            token = run {
                val random = SecureRandom()
                val bytes = ByteArray(32)
                random.nextBytes(bytes)
                Base64.encode(bytes)
            }
        )
    }
}
