package com.jaoafa.vcspeaker.voicetext

import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.VoiceChannel

object NarrationScripts {
    const val SELF_JOIN = "接続しました。"
    const val SELF_MOVE = "移動しました。"

    fun userJoined(member: Member) = userJoined(member.displayName)

    private fun userJoined(name: String) = "$name が参加しました。"

    fun userLeft(member: Member) = userLeft(member.displayName)

    private fun userLeft(name: String) = "$name が退出しました。"

    fun userJoinedOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userJoinedOtherChannel(member.displayName, (channel as VoiceChannel).name)

    private fun userJoinedOtherChannel(name: String, channel: String) = "$name が $channel に参加しました。"

    fun userLeftOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userLeftOtherChannel(member.displayName, (channel as VoiceChannel).name)

    private fun userLeftOtherChannel(name: String, channel: String) = "$name が $channel から退出しました。"

    fun userAfk(member: Member) = userAfk(member.displayName)

    private fun userAfk(name: String) = "$name が AFK になりました。"

    fun userAfkReturned(member: Member) = userAfkReturned(member.displayName)

    private fun userAfkReturned(name: String) = "$name が AFK から戻りました。"

    fun userAfkReturnedOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userAfkReturnedOtherChannel(member.displayName, (channel as VoiceChannel).name)

    private fun userAfkReturnedOtherChannel(name: String, channel: String) =
        "$name が AFK から $channel へ戻りました。"
}