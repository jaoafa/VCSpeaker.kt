package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Message
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MessageSerializer : KSerializer<Message> {
    override val descriptor = SerialDescriptor("dev.kord.core.entity.Guild", MessageData.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: Message) {
        encoder.encodeSerializableValue(MessageData.serializer(), value.data)
    }

    override fun deserialize(decoder: Decoder): Message {
        val data = decoder.decodeSerializableValue(MessageData.serializer())

        return Message(data, VCSpeaker.kord)
    }
}