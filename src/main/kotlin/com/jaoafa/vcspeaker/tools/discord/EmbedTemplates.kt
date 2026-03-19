package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor

object EmbedTemplates {
    val GuildNotRegistered: EmbedBuilderLambda = {
        title = ":x: Guild Not Registered"
        description = "サーバーが登録されていません。まず `/vcspeaker register` で登録してください。"

        errorColor()
    }
}