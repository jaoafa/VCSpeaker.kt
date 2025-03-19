package com.jaoafa.vcspeaker.tts.processors

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isThread
import com.jaoafa.vcspeaker.tts.Voice
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel

class MessageTransferProcessor : BaseProcessor() {
    override val priority = 20

    override suspend fun process(message: Message?, content: String, voice: Voice): Pair<String, Voice> {
        if (message?.type != MessageType.Default) return content to voice
        if (message.flags?.contains(MessageFlag.fromShift(14)) != true) return content to voice // TODO: 14 is MessageFlags.HAS_SNAPSHOT

        val messageReference = message.messageReference ?: return content to voice

        val targetChannel = messageReference.channel.fetchChannelOrNull()
            ?: return "どこかのチャンネルで送信したメッセージの引用" to voice

        val channelType = getChannelTypeText(targetChannel)

        val channelReplaceTo = if (targetChannel.isThread()) {
            val targetThread = targetChannel.asChannelOf<ThreadChannel>()
            "$channelType「${targetThread.parent.asChannel().name}」のスレッド「${targetThread.name}」で送信したメッセージ"
        } else if (targetChannel is TextChannel) {
            "$channelType「${targetChannel.name}」で送信したメッセージ"
        } else {
            "$channelType で送信したメッセージ"
        }

        val targetMessage =
            messageReference.message?.fetchMessageOrNull() ?: return channelReplaceTo + "の引用" to voice

        val messageContent = targetMessage.content

        val messageReplaceTo = if (messageContent.length > 50) {
            val messageContentHead = messageContent.substring(0, 50)
            "「$messageContentHead 以下略」の引用"
        } else {
            "「$messageContent」の引用"
        }

        return "$channelReplaceTo、$messageReplaceTo" to voice
    }

    /**
     * チャンネルタイプに応じて、読み上げるチャンネル種別テキストを返します。
     */
    private fun getChannelTypeText(channel: MessageChannel) = when (channel.type) {
        is ChannelType.GuildText -> "テキストチャンネル"
        is ChannelType.GuildVoice -> "ボイスチャンネル"
        is ChannelType.GuildCategory -> "カテゴリ"
        is ChannelType.GuildNews -> "ニュースチャンネル"
        is ChannelType.PublicNewsThread -> "ニューススレッド"
        is ChannelType.PublicGuildThread -> "スレッド"
        is ChannelType.PrivateThread -> "プライベートスレッド"
        is ChannelType.GuildStageVoice -> "ステージチャンネル"
        is ChannelType.GuildDirectory -> "ディレクトリ"
        is ChannelType.GuildForum -> "フォーラム"
        is ChannelType.GuildMedia -> "メディアチャンネル"
        is ChannelType.DM -> "DMチャンネル"
        is ChannelType.GroupDM -> "グループDMチャンネル"
        else -> channel.type.toString() + "チャンネル"
    }
}