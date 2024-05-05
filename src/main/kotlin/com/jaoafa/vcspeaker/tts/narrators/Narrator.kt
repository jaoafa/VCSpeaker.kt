package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tts.MessageProcessor.processMessage
import com.jaoafa.vcspeaker.tts.Scheduler
import com.jaoafa.vcspeaker.tts.TextProcessor.extractInlineVoice
import com.jaoafa.vcspeaker.tts.TextProcessor.processText
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.kotlindiscord.kord.extensions.utils.deleteOwnReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 読み上げを管理するクラスです。
 *
 * @param guildId サーバー ID
 * @param player Lavaplayer の [AudioPlayer] インスタンス
 * @param connection [VoiceConnection] インスタンス
 */
class Narrator @OptIn(KordVoice::class) constructor(
    val guildId: Snowflake,
    val player: AudioPlayer,
    val connection: VoiceConnection
) {
    companion object {
        suspend fun Guild.announce(
            voice: String,
            text: String,
            replier: (suspend (String) -> Unit)? = null,
            isOnlyMessage: Boolean = false,
        ) {
            if (replier != null) {
                replier(text)
            } else {
                val channel = GuildStore.getOrDefault(id).channelId?.asChannelOf<TextChannel>()
                channel?.createMessage(text)
            }

            if (!isOnlyMessage) {
                narrator()?.queueSelf(voice)
            }
        }
    }

    private val scheduler = Scheduler(player)

    /**
     * システム音声として文章をキューに追加します。
     *
     * @param text 読み上げる文章
     */
    suspend fun queueSelf(text: String) =
        queue(text = text, voice = GuildStore.getOrDefault(guildId).voice)

    /**
     * ユーザーの発言としてメッセージをキューに追加します。
     *
     * @param message 読み上げるメッセージ
     */
    suspend fun queueUser(message: Message) =
        queue(message = message, voice = VoiceStore.byIdOrDefault(message.author!!.id))

    /**
     * 読み上げをキューに追加します。
     *
     * @param message 読み上げるメッセージ
     * @param text 読み上げる文章
     * @param voice 読み上げに使用する音声
     */
    private suspend fun queue(message: Message? = null, text: String? = null, voice: Voice) {
        val content = processMessage(message) ?: text ?: return

        // extract inline voice
        val (extractedText, inlineVoice) = extractInlineVoice(content, voice)

        // process text
        val replacedText = processText(guildId, extractedText) ?: return

        if (replacedText.isBlank()) return

        CoroutineScope(Dispatchers.Default).launch {
            message?.addReaction("👀")
        }

        scheduler.queue(message, replacedText, inlineVoice)

        CoroutineScope(Dispatchers.Default).launch {
            message?.deleteOwnReaction("👀")
        }
    }

    /**
     * 読み上げ中のメッセージをスキップします。
     */
    suspend fun skip() = scheduler.skip()

    /**
     * キューをクリアします。
     */
    suspend fun clear() {
        CoroutineScope(Dispatchers.Default).launch {
            listOfNotNull(*scheduler.queue.toTypedArray(), scheduler.now).forEach {
                it.message?.deleteOwnReaction(ReactionEmoji.Unicode("🔊"))
                it.message?.deleteOwnReaction(ReactionEmoji.Unicode("👀"))
            }
        }

        scheduler.queue.clear()
        scheduler.now = null
        player.stopTrack()
    }

    suspend fun announce(
        voice: String,
        text: String,
        replier: (suspend (String) -> Unit)? = null,
    ) {
        val guild = VCSpeaker.kord.getGuildOrNull(guildId)

        guild?.announce(voice, text, replier)
    }

    init {
        player.addListener(scheduler)
    }
}