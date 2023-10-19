package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.CommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.voicetext.Narrators.narrator
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand

class ClearCommand : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("clear", "予定されているメッセージの読み上げを中止します。") {
            action {
                val narrator = guild?.narrator() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.queueSelf("読み上げを中止しました。")

                respond("**:white_check_mark: 予定されていたメッセージの読み上げを中止しました。**")
            }
        }

        chatCommand {
            name = "clear"
            description = "予定されているメッセージの読み上げを中止します。"

            action {
                val narrator = guild?.narrator() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.queueSelf("読み上げを中止しました。")

                respond("**:white_check_mark: 予定されていたメッセージの読み上げを中止しました。**")
            }
        }
    }
}