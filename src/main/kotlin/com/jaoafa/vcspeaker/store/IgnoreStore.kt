package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.VCSpeaker
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class IgnoreData(
    val guildId: Long,
    val userId: Long,
    val string: String
)

object IgnoreStore : StoreStruct<IgnoreData>(
    VCSpeaker.Files.ignores.path,
    IgnoreData.serializer(),
    { Json.decodeFromString(this) }
) {

}