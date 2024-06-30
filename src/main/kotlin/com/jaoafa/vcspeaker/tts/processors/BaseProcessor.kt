package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.core.entity.Message

/**
 * メッセージを処理する基底クラス
 */
abstract class BaseProcessor {
    abstract val priority: Int
    private var isCancelled: Boolean = false
    private var isImmediatelyRead: Boolean = false

    abstract suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice>

    fun isCancelled() = isCancelled
    fun isImmediately() = isImmediatelyRead

    fun cancel() {
        isCancelled = true
    }

    fun immediatelyRead() {
        isImmediatelyRead = true
    }
}