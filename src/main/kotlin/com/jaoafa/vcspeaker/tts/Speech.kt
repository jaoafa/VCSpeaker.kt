package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message

/**
 * メッセージの読み上げ情報を保持するクラスです。
 * 文章内のコンポーネントが複数の SpeechProvider に依存する場合、複数の [ProviderContext] を持つことができます。
 *
 * @param actor 読み上げの種類
 * @param guild サーバー
 * @param message メッセージ
 * @param contexts [ProviderContext] のリスト
 * @param tracks [AudioTrack] のリスト
 */
data class Speech(
    val actor: SpeechActor,
    val guild: Guild,
    val message: Message? = null,
    val contexts: List<ProviderContext>,
    val tracks: List<AudioTrack>
) {
    private var index: Int = 0

    /**
     * 次の [AudioTrack] を取得します。
     *
     * @return [AudioTrack] が存在しない場合は null
     */
    fun next(): AudioTrack? {
        index++

        if (index >= tracks.size) return null
        return tracks[index]
    }

    fun describe(withText: Boolean = false): String {
        val optionalText = if (withText) " \"${contexts.map { it.describe() }}\"" else ""

        return when (actor) {
            SpeechActor.System -> "the system message$optionalText"
            SpeechActor.User -> "the message$optionalText by @${message?.author?.username ?: "unknown_member"}"
        }
    }
}