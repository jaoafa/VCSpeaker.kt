package com.jaoafa.vcspeaker.tts.narrators

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.addReactionSafe
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.deleteOwnReactionSafe
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.Scheduler
import com.jaoafa.vcspeaker.tts.SpeechActor
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.jaoafa.vcspeaker.tts.processors.BaseProcessor
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.voice.VoiceConnection
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

    val scheduler = Scheduler(player)

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
            actor = SpeechActor.System
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
            actor = SpeechActor.User
        )

    /**
     * 読み上げをキューに追加します。
     *
     * @param message 読み上げる対象メッセージ
     * @param text 読み上げる文章
     * @param voice 読み上げに使用する音声
     * @param guild サーバー
     * @param actor 読み上げの種類
     */
    private suspend fun schedule(
        message: Message? = null,
        text: String,
        voice: Voice,
        guild: Guild,
        actor: SpeechActor
    ) {
        val sounds = Regex("<sound:\\d+:(\\d+)>").findAll(text).mapNotNull {
            val id = it.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            it.value to id
        }

        val textElements = text.split(*(sounds.map { it.first }.toList().toTypedArray()))

        val contexts = mutableListOf<ProviderContext>()

        textElements.forEachIndexed { i, element ->
            val (processText, processVoice) = process(message, element, voice) ?: return

            if (processText.isNotBlank())
                contexts.add(VoiceTextContext(processVoice, processText))

            if (i < sounds.count())
                contexts.add(SoundmojiContext(Snowflake(sounds.elementAt(i).second)))
        }

        if (contexts.isEmpty()) return

        message?.addReactionSafe("👀")

        scheduler.queue(contexts, message, guild, actor)

        message?.deleteOwnReactionSafe("👀")
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
            println("Processed by ${processor::class.simpleName}: $processedText [isCancelled=${processor.isCancelled()}, isImmediately=${processor.isImmediately()}]")
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
    fun clear() {
        listOfNotNull(*scheduler.queue.toTypedArray(), scheduler.current()).forEach {
            it.message?.deleteOwnReactionSafe("🔊")
            it.message?.deleteOwnReactionSafe("👀")
        }

        scheduler.queue.clear()
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