package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.reload.Reload
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import java.io.File

class UpdateCommand : Extension() {
    override val name = this::class.simpleName!!

    inner class UpdateOptions : Options() {
        val path by string {
            name = "file"
            description = "アップデート先のファイル名"
        }
    }

    override suspend fun setup() {
        publicSlashCommand("update", "VCSpeaker を更新します (デバッグ用)", ::UpdateOptions) {
            check {
                failIfNot(VCSpeaker.isDev(), "このコマンドは開発モードでのみ使用できます。")
            }
            action {
                val file = File("./updates/${arguments.path}")

                if (!file.exists()) {
                    respond("ファイル `${arguments.path}` が存在しません。")
                    return@action
                }

                respond("`${arguments.path}` にアップデートしています...")

                Reload.updateTo(file)
            }
        }
    }
}