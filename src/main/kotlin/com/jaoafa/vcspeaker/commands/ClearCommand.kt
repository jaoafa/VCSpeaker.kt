package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respond
import com.kotlindiscord.kord.extensions.extensions.Extension

class ClearCommand : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("clear", "読み上げ予定のメッセージを全て中止します。") {
            action {
                val narrator = VCSpeaker.narrators[guild!!.id] ?: run {
                    respond("**:question: VC に参加していません。**")
                    return@action
                }

                narrator.clear()
                narrator.queueSelf("")

                respond("**:white_check_mark: キューをクリアしました。**")
            }
        }
    }
}