package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.tools.discord.ChatCommandExtensions.chatCommand
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tts.narrators.Narrators.narrator
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class ClearCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        publicSlashCommand("clear", "予定されているメッセージの読み上げを中止します。") {
            check { anyGuild() }
            action {
                val narrator = guild?.narrator() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.scheduleAsSystem("読み上げを中止しました。")

                respond("**:white_check_mark: 予定されていたメッセージの読み上げを中止しました。**")

                log(logger) { guild, user -> "[${guild.name}] All scheduled messages are cleared by @${user.username}." }
            }
        }

        chatCommand("clear", "予定されているメッセージの読み上げを中止します。") {
            check {
                anyGuild()
                isNotBot()
            }
            action {
                val narrator = guild?.narrator() ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.scheduleAsSystem("読み上げを中止しました。")

                respond("**:white_check_mark: 予定されていたメッセージの読み上げを中止しました。**")

                log(logger) { guild, user -> "[${guild.name}] All scheduled messages are cleared by @${user.username}." }
            }
        }
    }
}