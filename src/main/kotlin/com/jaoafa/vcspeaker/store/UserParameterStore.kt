package com.jaoafa.vcspeaker.store

import com.jaoafa.vcspeaker.tools.readOrCreateAs
import com.jaoafa.vcspeaker.voicetext.Speaker
import com.jaoafa.vcspeaker.voicetext.VoiceParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
data class UserParameterData(
    val userId: Long,
    val parameter: VoiceParameter
)

object UserParameterStore : StoreStruct<UserParameterData> {
    override val data = File("./user_parameters.json").readOrCreateAs(
        ListSerializer(UserParameterData.serializer()),
        mutableListOf()
    )

    fun byId(userId: Long) = data.find { it.userId == userId }?.parameter

    fun byIdOrDefault(userId: Long) = byId(userId) ?: VoiceParameter(speaker = Speaker.HIKARI)
}