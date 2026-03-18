package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.diffUpsert
import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.AliasRow
import com.jaoafa.vcspeaker.database.tables.AliasTable
import com.jaoafa.vcspeaker.database.toTyped
import com.jaoafa.vcspeaker.features.Alias
import com.jaoafa.vcspeaker.features.Alias.fieldAliasFrom
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiUtils
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.PublicSlashCommandContext
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.converters.impl.int
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.capitalizeWords
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AliasCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    class CreateOptions : Options() {
        val type by stringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.entries)
                choice(aliasType.displayName, aliasType.name)
        }

        val search by string {
            name = "search"
            description = "置き換える条件"
        }

        val replace by string {
            name = "replace"
            description = "置き換える文字列"
        }
    }

    class UpdateOptions : Options() {
        val aliasId by int {
            name = "alias"
            description = "更新するエイリアス"

            autoComplete(Alias.autocomplete)
        }

        val type by optionalStringChoice {
            name = "type"
            description = "エイリアスの種類"
            for (aliasType in AliasType.entries)
                choice(aliasType.displayName, aliasType.name)
        }

        val search by optionalString {
            name = "search"
            description = "置き換える条件"
        }

        val replace by optionalString {
            name = "replace"
            description = "置き換える文字列"
        }
    }

    class DeleteOptions : Options() {
        val aliasId by int {
            name = "alias"
            description = "削除するエイリアス"

            autoComplete(Alias.autocomplete)
        }
    }

    @OptIn(AlwaysPublicResponse::class)
    override suspend fun setup() {
        publicSlashCommand("alias", "エイリアスを設定します。") {
            check { anyGuild() }
            publicSubCommand("create", "エイリアスを作成します。", ::CreateOptions) {
                action {
                    val guild = guild ?: return@action
                    val type = AliasType.valueOf(arguments.type)
                    val search = arguments.search
                    val replace = arguments.replace

                    if (!validateSoundboardAlias(type, replace)) return@action

                    val (old, new) = transaction {
                        AliasTable.diffUpsert {
                            it[guildDid] = guild.id
                            it[creatorDid] = user.id
                            it[this.type] = AliasType.valueOf(arguments.type)
                            it[this.search] = search
                            it[this.replace] = replace
                        }
                    }
                    val isUpdate = old != null && old != new
                    val oldReplace = old?.replace
                    val newReplace = new.replace

                    respondEmbed(
                        ":loudspeaker: Alias ${if (isUpdate) "Updated" else "Created"}",
                        "${type.displayName}のエイリアスを${if (isUpdate) "更新" else "作成"}しました"
                    ) {
                        authorOf(user)

                        fieldAliasFrom(type, search)

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            if (isUpdate) "$oldReplace → **$newReplace**" else newReplace
                        }

                        successColor()
                    }

                    val verb = if (isUpdate) "updated" else "created"

                    log(logger) { guild, user ->
                        "[${guild.name}] Alias ${verb.capitalizeWords()}: @${user.username} $verb alias: $new" +
                                if (isUpdate) " (updated from \"$old\")" else ""
                    }
                }
            }

            publicSubCommand("update", "エイリアスを更新します。", ::UpdateOptions) {
                action {
                    val aliasEntity = transaction {
                        AliasEntity.findById(arguments.aliasId)
                    }
                    val oldRow = transaction {
                        aliasEntity?.readValues?.toTyped<AliasRow>()
                    }

                    if (aliasEntity == null || oldRow == null) {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "エイリアスが見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Not Found: @${user.username} attempted to update alias \"${arguments.aliasId}\" but not found"
                        }

                        return@action
                    }

                    val updatedType =
                        arguments.type?.let { typeString -> AliasType.valueOf(typeString) } ?: oldRow.type
                    val updatedSearch = arguments.search ?: oldRow.search
                    val updatedReplace = arguments.replace ?: oldRow.replace

                    if (!validateSoundboardAlias(updatedType, updatedReplace)) return@action

                    try {
                        transaction {
                            aliasEntity.type = updatedType
                            aliasEntity.search = updatedSearch
                            aliasEntity.replace = updatedReplace
                            aliasEntity.version += 1
                        }
                    } catch (e: ExposedSQLException) {
                        when (e.cause) {
                            is JdbcSQLIntegrityConstraintViolationException -> {
                                respondEmbed(
                                    ":x: Duplicated Alias",
                                    "「$updatedSearch」を置き換えるエイリアスはすでに存在します。"
                                ) {
                                    authorOf(user)
                                    errorColor()
                                }
                                log(logger) { guild, user ->
                                    "[${guild.name}] Duplicated Alias: @${user.username} attempted to update $oldRow with $arguments but failed due to duplication."
                                }
                            }

                            else -> throw e
                        }

                        return@action
                    }


                    val newRow = transaction {
                        aliasEntity.readValues.toTyped<AliasRow>()
                    }

                    respondEmbed(
                        ":repeat: Alias Updated",
                        "エイリアスを更新しました。"
                    ) {
                        authorOf(user)

                        fun searchDisplay(type: AliasType, search: String) = when (type) {
                            AliasType.Text -> search
                            AliasType.Regex -> "`$search`"
                            AliasType.Emoji -> "$search `$search`"
                            AliasType.Soundboard -> search
                        }

                        field("${updatedType.emoji} ${updatedType.displayName}", true) {
                            searchDisplay(oldRow.type, oldRow.search) + if (oldRow.search != newRow.search)
                                " → **${searchDisplay(newRow.type, newRow.search)}**"
                            else ""
                        }

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            if (oldRow.replace != newRow.replace)
                                "「${oldRow.replace}」→「**${newRow.replace}**」"
                            else
                                "「${newRow.replace}」"
                        }

                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Alias Updated: @${user.username} updated alias $oldRow -> $newRow"
                    }
                }
            }

            publicSubCommand("delete", "エイリアスを削除します。", ::DeleteOptions) {
                action {
                    val aliasEntity = transaction {
                        AliasEntity.findById(arguments.aliasId)
                    }

                    if (aliasEntity == null) {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "エイリアスが見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Not Found: @${user.username} attempted to delete alias \"${arguments.aliasId}\" but not found"
                        }

                        return@action
                    }

                    val row = transaction {
                        aliasEntity.getRow()
                    }

                    transaction {
                        aliasEntity.delete()
                    }
                    respondEmbed(
                        ":wastebasket: Alias Deleted",
                        "${row.type.displayName}エイリアスを削除しました。"
                    ) {
                        authorOf(user)

                        fieldAliasFrom(row.type, row.search)

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            row.replace
                        }

                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Alias Deleted: @${user.username} deleted $row"
                    }
                }
            }

            publicSubCommand("list", "エイリアスの一覧を表示します。") {
                action {
                    val guildId = guild?.id ?: return@action
                    val aliasEntities = transaction {
                        AliasEntity.find { AliasTable.guildDid eq guildId }.map { it.getRow() }
                    }

                    if (aliasEntities.isEmpty()) {
                        respondEmbed(
                            ":grey_question: Aliases Not Found",
                            "エイリアスが設定されていないようです。`/alias create` で作成してみましょう！"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    respondingPaginator {
                        for (chunkedAliases in aliasEntities.chunked(10)) {
                            page {
                                authorOf(user)

                                title = ":information_source: Aliases"

                                description = chunkedAliases.joinToString("\n") {
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

    private suspend fun PublicSlashCommandContext<*, *>.validateSoundboardAlias(
        type: AliasType,
        replace: String
    ): Boolean {
        if (type != AliasType.Soundboard) return true
        if (SoundmojiUtils.containsSoundmojiReference(replace)) return true

        respondEmbed(
            ":x: Invalid Soundboard",
            "サウンドボードのURL、`<sound:0:ID>`、もしくはIDのみを指定してください。"
        ) {
            authorOf(user)
            errorColor()
        }

        return false
    }
}
