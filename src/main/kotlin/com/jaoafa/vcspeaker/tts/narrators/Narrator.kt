package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.Scheduler
import com.jaoafa.vcspeaker.tts.TrackType
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.jaoafa.vcspeaker.tts.processors.BaseProcessor
import dev.kordex.core.utils.addReaction
import dev.kordex.core.utils.deleteOwnReaction
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.full.createInstance

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

            if (!isOnlyMessage)
                narrator()?.scheduleAsSystem(voice)
        }
    }

    private val scheduler = Scheduler(guildId, player)

    /**
     * システム音声として文章をキューに追加します。
     *
     * @param text 読み上げる文章
     */
    suspend fun scheduleAsSystem(text: String) =
        schedule(
            text = text,
            voice = GuildStore.getOrDefault(guildId).voice,
            guild = VCSpeaker.kord.getGuild(guildId),
            type = TrackType.System
        )

    /**
     * ユーザーの発言としてメッセージをキューに追加します。
     *
     * @param message 読み上げるメッセージ
     */
    suspend fun scheduleAsUser(message: Message) =
        schedule(
            message = message,
            text = message.content,
            voice = VoiceStore.byIdOrDefault(message.author!!.id),
            guild = message.getGuild(),
            type = TrackType.User
        )

    /**
     * 読み上げをキューに追加します。
     *
     * @param message 読み上げるメッセージ
     * @param text 読み上げる文章
     * @param voice 読み上げに使用する音声
     */
    private suspend fun schedule(
        message: Message? = null,
        text: String,
        voice: Voice,
        guild: Guild,
        type: TrackType
    ) {
        val (processText, processVoice) = process(message, text, voice) ?: return

        if (processText.isBlank()) return

        CoroutineScope(Dispatchers.Default).launch {
            message?.addReaction("👀")
        }

        scheduler.queue(message, processText, processVoice, guild, type)

        CoroutineScope(Dispatchers.Default).launch {
            message?.deleteOwnReaction("👀")
        }
    }

    /**
     * テキストを処理します。
     *
     * @param message メッセージ
     * @param text 処理するテキスト
     * @param voice 処理する音声
     * @return 処理後のテキストと音声。キャンセルされた場合は null が返却されます。
     */
    suspend fun process(message: Message? = null, text: String, voice: Voice): Pair<String, Voice>? {
        val processors = getClassesIn<BaseProcessor>("com.jaoafa.vcspeaker.tts.processors")
            .mapNotNull {
                it.kotlin.createInstance()
            }.sortedBy { it.priority }

        return processors.fold(text to voice) { (processText, processVoice), processor ->
            val (processedText, processedVoice) = processor.process(message, processText, processVoice)
            if (processor.isCancelled()) return null // キャンセルされた場合は、即座に null を返却。
            if (processor.isImmediately()) return processedText to processedVoice // 即座に返す場合は、このProcessorを最後とし読み上げる

            processedText to processedVoice
        }
    }

    /**
     * 読み上げ中のメッセージをスキップします。
     */
    fun skip() = scheduler.skip()

    /**
     * キューをクリアします。
     */
    fun clear() = scheduler.clear()

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