package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
data class IgnoreData(
    val guildId: Long,
    val userId: Long,
    val string: String
)

object IgnoreStore : StoreStruct<IgnoreData> {
    override val data = File("./ignores.json").readOrCreateAs(
        ListSerializer(IgnoreData.serializer()),
        mutableListOf()
    )
}