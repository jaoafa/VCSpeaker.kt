package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.voicetext.MessageProcessor.processMessage
import com.jaoafa.vcspeaker.voicetext.TextProcessor.extractInlineVoice
import com.jaoafa.vcspeaker.voicetext.TextProcessor.processText
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Narrator @OptIn(KordVoice::class) constructor(
    val guildId: Snowflake,
    val player: AudioPlayer,
    val connection: VoiceConnection
) {
    private val scheduler = NarratorScheduler(guildId, player)

    private suspend fun queue(message: Message? = null, text: String? = null, voice: Voice) {
        val content = processMessage(message) ?: text ?: return

        val (extractedText, inlineVoice) = extractInlineVoice(content, voice)

        val replacedText = processText(guildId, extractedText) ?: return

        if (replacedText.isBlank()) return

        scheduler.queue(SpeakInfo(replacedText, inlineVoice, message))
    }

    suspend fun queueSelf(text: String) =
        queue(text = text, voice = GuildStore.getOrDefault(guildId).voice)

    suspend fun queueUser(message: Message) =
        queue(message = message, voice = VoiceStore.byIdOrDefault(message.author!!.id))


    suspend fun skip() = scheduler.skip()

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

    init {
        player.addListener(scheduler)
    }
}