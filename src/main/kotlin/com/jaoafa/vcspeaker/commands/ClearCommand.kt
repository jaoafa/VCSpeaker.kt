package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.devGuild
import com.jaoafa.vcspeaker.tools.publicSlashCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respond

class ClearCommand : Extension() {

    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand("clear", "読み上げ予定のメッセージを全て中止します。") {
            action {
                val narrator = VCSpeaker.narrators[guild!!.id] ?: run {
                    respond { content = "**:question: VC に参加していません。**" }
                    return@action
                }

                narrator.clear()
                narrator.queueSelf("")

                respond { content = "**:white_check_mark: キューをクリアしました。**" }
            }
        }
    }
}