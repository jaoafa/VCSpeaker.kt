package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tools.discord.MessageSerializer
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.core.entity.Message
import kotlinx.serialization.Serializable

/**
 * メッセージの読み上げ情報を保持するクラスです。
 * 文章内のコンポーネントが複数の SpeechProvider に依存する場合、複数の [ProviderContext] を持つことができます。
 *
 * @param actor 読み上げの種類
 * @param guildName サーバー名
 * @param message メッセージ
 * @param contexts [ProviderContext] のリスト
 * @param tracks [AudioTrack] のリスト
 */
@Serializable
data class Speech(
    val actor: SpeechActor,
    val guildName: String,
    @Serializable(with = MessageSerializer::class)
    val message: Message? = null,
    val contexts: List<ProviderContext>,
    val tracks: List<AudioTrack>
) {
    private var index: Int = 0

    /**
     * 次の [AudioTrack] と [ProviderContext] を取得します。
     *
     * @return [Pair<AudioTrack, ProviderContext>], 存在しない場合は null
     */
    fun next(): Pair<AudioTrack, ProviderContext>? {
        index++

        if (index >= tracks.size) return null
        return tracks[index] to contexts[index]
    }

    fun describe(withText: Boolean = false): String {
        val optionalText = if (withText) " \"${contexts.map { it.describe() }}\"" else ""

        return when (actor) {
            SpeechActor.System -> "the system message$optionalText"
            SpeechActor.User -> "the message$optionalText by @${message?.author?.username ?: "unknown_member"}"
        }
    }
}