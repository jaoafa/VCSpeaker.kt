package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.ChatCommandExtensions.chatCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import com.kotlindiscord.kord.extensions.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class ClearCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

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

                logger.info { "All scheduled messages are cleared." }
            }
        }

        chatCommand("clear", "予定されているメッセージの読み上げを中止します。") {
            action {
                val narrator = guild?.narrator() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.queueSelf("読み上げを中止しました。")

                respond("**:white_check_mark: 予定されていたメッセージの読み上げを中止しました。**")
                logger.info { "All scheduled messages are cleared." }
            }
        }
    }
}