package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.markdown.LineEffect
import com.jaoafa.vcspeaker.tts.markdown.toMarkdown
import dev.kord.core.entity.Message

class MarkdownHeadingProcessor : BaseProcessor() {
    override val priority = 100

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        val markdown = content.toMarkdown()

        // 1行で、ヘッダー行であること
        if (markdown.size != 1 || (markdown[0].effects.isNotEmpty() && markdown[0].effects.first() != LineEffect.Header)) {
            return content to voice
        }

        // inlines[0].text がヘッダーの文字列
        // 先頭から始まる "#" の数を取得する
        val header = markdown[0].inlines[0].text
        val headerLevel = header.takeWhile { it == '#' }.count()

        // ヘッダーのレベルに応じて音声の速度を変更する
        // 0: 100%
        // 1: 200%
        // 2: 175%
        // 3: 150%
        val newVoice = when (headerLevel) {
            1 -> voice.copy(speed = 200)
            2 -> voice.copy(speed = 175)
            3 -> voice.copy(speed = 150)
            else -> voice
        }

        val newContent = markdown.joinToString {
            // ヘッダー行の場合、フォーマット文字列を削除する
            if (it.effects.isNotEmpty() && it.effects.first() != LineEffect.Header) {
                return@joinToString it.inlines.joinToString("") { inline ->
                    inline.text
                }.drop(headerLevel).trim()
            }
            // ヘッダー行以外の場合、そのまま文字列を結合して返す
            it.inlines.joinToString("") { inline -> inline.text }
        }.drop(headerLevel).trim()

        return newContent to newVoice
    }
}