package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import dev.kord.core.entity.channel.Channel

object EmbedTemplates {
    class NotInGuild : EmbedTemplate({
        title = ":x: Not In Guild"
        description = "このコマンドはサーバー内でのみ使用できます。"

        errorColor()
    })

    class GuildNotRegistered : EmbedTemplate({
        title = ":x: Guild Not Registered"
        description = "サーバーが登録されていません。まず `/vcspeaker register` で登録してください。"

        errorColor()
    })

    class InvalidChannel(channel: Channel) : EmbedTemplate({
        title = ":face_with_symbols_over_mouth: Invalid Channel"
        description = "${channel.mention} は有効なテキストチャンネルではありません。"

        errorColor()
    })

    class InsufficientChannelPermissions(channel: Channel) : EmbedTemplate({
        title = ":x: Insufficient Permissions"
        description = "この操作を実行するには、${channel.mention} の管理権限が必要です。"

        errorColor()
    })
}