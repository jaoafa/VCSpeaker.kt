package com.jaoafa.vcspeaker.voicetext

import dev.kord.core.entity.Message

object MessageProcessor {

    fun processMessage(message: Message?): String? {
        if (message == null) return null

        val stickers = message.stickers
        val attachments = message.attachments
        val content = message.content

        if (stickers.isNotEmpty())
            return stickers.joinToString(" ") { "スタンプ ${it.name}" }

        if (attachments.isNotEmpty()) {
            val firstAttachment = attachments.toList().first()
            val fileType = if (firstAttachment.isImage) "画像" else "添付"

            val firstFileRead = "${fileType}ファイル ${firstAttachment.filename}"

            val fullFileRead =
                if (attachments.size > 1)
                    "$firstFileRead と${attachments.size - 1}個のファイル"
                else firstFileRead

            return if (content.isBlank()) fullFileRead else "$content $fullFileRead"
        }

        return content.ifBlank { null }
    }
}