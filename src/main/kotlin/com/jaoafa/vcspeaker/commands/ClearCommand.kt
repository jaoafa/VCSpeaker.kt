package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tools.devGuild
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond

class ClearCommand : Extension() {
    override val name = this::class.simpleName!!

    override suspend fun setup() {
        publicSlashCommand {
            name = "clear"
            description = "読み上げ予定のメッセージをキューから削除します。"

            devGuild()

            check {
                isNotBot()
            }

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