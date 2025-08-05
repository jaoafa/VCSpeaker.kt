package com.jaoafa.vcspeaker.api.types

import kotlinx.serialization.Serializable

/**
 * Latest から Current へ更新が完了したことを通知するペイロード
 *
 * @param id Latest 側の ID
 * @param token Latest 側のトークン
 */
@Serializable
data class InitFinishedRequest(
    val id: String,
    val token: String
)