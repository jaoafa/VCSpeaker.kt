package com.jaoafa.vcspeaker.tts

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.name
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.entity.Member

object NarrationScripts {
    const val SELF_JOIN = "接続しました。"
    const val SELF_MOVE = "移動しました。"

    fun userJoined(member: Member) = userJoined(member.effectiveName)

    private fun userJoined(name: String) = "${name}が参加しました。"

    fun userLeft(member: Member) = userLeft(member.effectiveName)

    private fun userLeft(name: String) = "${name}が退出しました。"

    suspend fun userJoinedOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userJoinedOtherChannel(member.effectiveName, channel.name())

    private fun userJoinedOtherChannel(name: String, channel: String) = "${name}が${channel}に参加しました。"

    suspend fun userLeftOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userLeftOtherChannel(member.effectiveName, channel.name())

    private fun userLeftOtherChannel(name: String, channel: String) = "${name}が${channel}から退出しました。"

    fun userAfk(member: Member) = userAfk(member.effectiveName)

    private fun userAfk(name: String) = "${name}がAFKになりました。"

    fun userAfkReturned(member: Member) = userAfkReturned(member.effectiveName)

    private fun userAfkReturned(name: String) = "${name}がAFKから戻りました。"

    suspend fun userAfkReturnedOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userAfkReturnedOtherChannel(member.effectiveName, channel.name())

    private fun userAfkReturnedOtherChannel(name: String, channel: String) =
        "${name}がAFKから${channel}へ戻りました。"

    fun userStartGoLive(member: Member) =
        userStartGoLive(member.effectiveName)

    private fun userStartGoLive(name: String) =
        "${name}がGoLiveを開始しました。"

    suspend fun userStartGoLiveOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userStartGoLiveOtherChannel(member.effectiveName, channel.name())

    private fun userStartGoLiveOtherChannel(name: String, channel: String) =
        "${name}が${channel}でGoLiveを開始しました。"

    fun userEndGoLive(member: Member) =
        userEndGoLive(member.effectiveName)

    private fun userEndGoLive(name: String) =
        "${name}がGoLiveを終了しました。"

    suspend fun userEndGoLiveOtherChannel(member: Member, channel: BaseVoiceChannelBehavior) =
        userEndGoLiveOtherChannel(member.effectiveName, channel.name())

    private fun userEndGoLiveOtherChannel(name: String, channel: String) =
        "${name}が${channel}でGoLiveを終了しました。"
}