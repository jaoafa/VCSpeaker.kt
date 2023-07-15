package com.jaoafa.vcspeaker

object NarrationScripts {
    fun selfJoin(channel: String) = "$channel に参加しました。"
    fun userJoin(name: String) = "$name が参加しました。"
    fun userLeave(name: String) = "$name が退出しました。"
    fun userJoinOtherChannel(name: String, channel: String) = "$name が $channel に参加しました。"
    fun userLeaveOtherChannel(name: String, channel: String) = "$name が $channel から退出しました。"
    fun userMoved(name: String, from: String, to: String) = "$name が $from から $to に移動しました。"
}