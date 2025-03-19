package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.common.entity.MessageType
import dev.kord.core.entity.Message
import dev.kord.core.entity.effectiveName

class PinMessageProcessor : BaseProcessor() {
    override val priority = 30

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        if (message?.type != MessageType.ChannelPinnedMessage) return content to voice

        val executorUserName = message.author?.effectiveName ?: "だれか"

        val messageReference = message.messageReference ?: return content to voice
        val pinnedMessageId = messageReference.message?.id ?: return content to voice
        val pinnedMessage = message.channel.getMessage(pinnedMessageId)
        val pinnedMessageUserName = pinnedMessage.author?.effectiveName ?: "だれか"

        return "$executorUserName が $pinnedMessageUserName のメッセージをピン止めしました" to voice
    }
}