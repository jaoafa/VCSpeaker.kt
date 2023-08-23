package com.jaoafa.vcspeaker.voicetext

object NarrationScripts {
    const val SELF_JOIN = "接続しました。"
    const val SELF_MOVE = "移動しました。"

    // todo user join/leave/move announce 
    fun userJoin(name: String) = "$name が参加しました。"
    fun userLeave(name: String) = "$name が退出しました。"
    fun userJoinOtherChannel(name: String, channel: String) = "$name が $channel に参加しました。"
    fun userLeaveOtherChannel(name: String, channel: String) = "$name が $channel から退出しました。"
    fun userMoved(name: String, from: String, to: String) = "$name が $from から $to に移動しました。"
}