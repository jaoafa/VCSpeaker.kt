package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntityOrNull
import com.jaoafa.vcspeaker.database.DatabaseUtil.isNotRegistered
import com.jaoafa.vcspeaker.database.tables.IgnoreEntity
import com.jaoafa.vcspeaker.database.tables.IgnoreTable
import com.jaoafa.vcspeaker.features.Ignore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbedOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.EmbedTemplates.GuildNotRegistered
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.converters.impl.int
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.api.ErrorCode.DUPLICATE_KEY_1
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class IgnoreCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    class CreateOptions : Options() {
        val type by stringChoice {
            name = "type"
            description = "無視をする文字列の検索条件"
            for (ignoreType in IgnoreType.entries)
                choice(ignoreType.displayName, ignoreType.name)
        }

        val search by string {
            name = "search"
            description = "無視する文字列"
        }
    }

    class DeleteOptions : Options() {
        val ignoreId by int {
            name = "ignore"
            description = "削除する無視条件"

            autoComplete(Ignore.autocomplete)
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("ignore", "無視機能を設定します。") {
            check { anyGuild() }
            publicSubCommand("create", "無視条件を作成します。", ::CreateOptions) {
                action {
                    val guild = guild ?: return@action

                    if (guild.isNotRegistered()) {
                        respondEmbedOf(GuildNotRegistered) {
                            authorOf(user)
                        }

                        return@action
                    }

                    val type = IgnoreType.valueOf(arguments.type)
                    val search = arguments.search

                    val row = try {
                        val entity = transaction {
                            val guildEntity = guild.getEntityOrNull()
                                ?: throw IllegalStateException("GuildEntity not found for guild ${guild.id}")
                            IgnoreEntity.new {
                                this.guildEntity = guildEntity
                                creatorDid = user.id
                                this.type = type
                                this.search = search
                            }
                        }
                        transaction {
                            entity.getRow()
                        }
                    } catch (e: ExposedSQLException) {
                        when (e.sqlState.toInt()) {
                            DUPLICATE_KEY_1 -> {
                                respondEmbed(
                                    ":x: Duplicated Ignore",
                                    "「$search」を無視するルールはすでに存在します。"
                                ) {
                                    authorOf(user)
                                    errorColor()
                                }
                                log(logger) { guild, user ->
                                    "[${guild.name}] Duplicated Ignore: @${user.username} attempted to create ignore with $arguments but failed due to duplication."
                                }
                            }

                            else -> throw e
                        }

                        return@action
                    }

                    val typeText = if (row.type == IgnoreType.Contains) "を含む" else "と一致する"

                    respondEmbed(
                        ":face_with_symbols_over_mouth: Ignore Created",
                        "今後「${row.search}」${typeText}メッセージは読み上げられません。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Ignore Created: @${user.username} created ignore $row"
                    }
                }
            }

            publicSubCommand("delete", "無視条件を削除します。", ::DeleteOptions) {
                action {
                    val ignoreEntity = transaction {
                        IgnoreEntity.findById(arguments.ignoreId)
                    }

                    if (ignoreEntity == null) {
                        respondEmbed(
                            ":question: Ignore Not Found",
                            "無視条件が見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Ignore Not Found: @${user.username} attempted to delete ignore ${arguments.ignoreId} but not found"
                        }

                        return@action
                    }

                    val row = transaction {
                        val row = ignoreEntity.getRow()
                        ignoreEntity.delete()
                        row
                    }

                    val typeText = if (row.type == IgnoreType.Contains) "が含まれて" else "と一致して"

                    respondEmbed(
                        ":wastebasket: Ignore Deleted",
                        "「${row.search}」${typeText}いても読み上げます。"
                    ) {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Ignore Deleted: @${user.username} deleted $row"
                    }
                }
            }

            publicSubCommand("list", "無視条件の一覧を表示します。") {
                action {
                    val guildId = guild?.id ?: return@action
                    val ignoreEntities = transaction {
                        IgnoreEntity.find { IgnoreTable.guildDid eq guildId }.map { it.getRow() }
                    }

                    if (ignoreEntities.isEmpty()) {
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
                        for (chunkedIgnores in ignoreEntities.chunked(10)) {
                            page {
                                authorOf(user)

                                title = ":information_source: Ignores"

                                description = chunkedIgnores.joinToString("\n") {
                                    it.describeWithEmoji()
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