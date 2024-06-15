package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.IgnoreData
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.kotlindiscord.kord.extensions.annotations.AlwaysPublicResponse
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringCollection
import io.github.oshai.kotlinlogging.KotlinLogging

class IgnoreCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class CreateOptions : Options() {
        val type by stringChoice {
            name = "type"
            description = "無視判定の種類"
            for (ignoreType in IgnoreType.entries)
                choice(ignoreType.displayName, ignoreType.name)
        }

        val text by string {
            name = "text"
            description = "無視する文字列"
        }
    }

    inner class DeleteOptions : Options() {
        val text by string {
            name = "text"
            description = "無視する文字列"

            autoComplete { event ->
                val guildId = event.interaction.getChannel().data.guildId.value

                suggestStringCollection(
                    IgnoreStore.filter(guildId).map { it.search },
                    FilterStrategy.Contains
                )
            }
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("ignore", "無視機能を設定します。") {
            check { anyGuild() }
            publicSubCommand("create", "無視する文字列を作成します。", ::CreateOptions) {
                action {
                    val type = IgnoreType.valueOf(arguments.type)
                    val text = arguments.text
                    val duplicateExists = IgnoreStore.find(guild!!.id, text) != null

                    if (!duplicateExists)
                        IgnoreStore.create(IgnoreData(guild!!.id, user.id, type, text))

                    val typeText = if (type == IgnoreType.Contains) "を含む" else "と一致する"

                    respondEmbed(
                        ":face_with_symbols_over_mouth: Ignore Created",
                        "今後「$text」${typeText}メッセージは読み上げられません。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    val typeName = type.name.lowercase()

                    log(logger) { guild, user ->
                        "[${guild.name}] Ignore Created: @${user.username} created ignore that ignores text that $typeName \"$text\""
                    }
                }
            }

            publicSubCommand("delete", "無視する文字列を削除します。", ::DeleteOptions) {
                action {
                    val text = arguments.text
                    val target = IgnoreStore.find(guild!!.id, text)

                    if (target != null) {
                        IgnoreStore.remove(target)

                        val typeText = if (target.type == IgnoreType.Contains) "が含まれて" else "と一致して"

                        respondEmbed(
                            ":wastebasket: Ignore Deleted",
                            "「$text」${typeText}いても読み上げます。"
                        ) {
                            authorOf(user)
                            successColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Ignore Deleted: @${user.username} deleted ignore that ignores text that ${target.type.name.lowercase()} \"${text}\""
                        }
                    } else {
                        respondEmbed(
                            ":question: Ignore Not Found",
                            """
                            「$text」に一致する設定が見つかりませんでした。
                            `/ignore list` で一覧を確認できます。    
                            """.trimIndent()
                        )

                        log(logger) { guild, user ->
                            "[${guild.name}] Ignore Not Found: @${user.username} searched for ignore that ignores text \"$text\" but not found"
                        }
                    }
                }
            }

            publicSubCommand("list", "無視する文字列の一覧を表示します。") {
                action {
                    val ignores = IgnoreStore.filter(guild!!.id)

                    if (ignores.isEmpty()) {
                        respondEmbed(
                            ":grey_question: Ignores Not Found",
                            "設定されていないようです。`/ignore create` で作成してみましょう！"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    respondingPaginator {
                        for (chunkedIgnores in ignores.chunked(10)) {
                            page {
                                authorOf(user)

                                title = ":information_source: Ignores"

                                description = chunkedIgnores.joinToString("\n") {
                                    it.toDisplayWithEmoji()
                                }

                                successColor()
                            }
                        }
                    }.send()
                }
            }
        }
    }
}