package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.DatabaseUtil.getEntity
import com.jaoafa.vcspeaker.database.DatabaseUtil.getRows
import com.jaoafa.vcspeaker.database.onDuplicate
import com.jaoafa.vcspeaker.database.transactionResulting
import com.jaoafa.vcspeaker.database.unwrap
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
import com.jaoafa.vcspeaker.tools.discord.anyGuildRegistered
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiUtils
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.commands.application.slash.PublicSlashCommandContext
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.converters.impl.int
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.jaoafa.vcspeaker.database.tables.AliasEntity as Entity
import com.jaoafa.vcspeaker.database.tables.AliasTable as Table

class AliasCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

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
            check { anyGuildRegistered() }
            publicSubCommand("create", "エイリアスを作成します。", ::CreateOptions) {
                action {
                    val guild = guild ?: return@action

                    val type = AliasType.valueOf(arguments.type)
                    val search = arguments.search
                    val replace = arguments.replace

                    if (!validateSoundboardAlias(type, replace)) return@action

                    val entity = transactionResulting {
                        Entity.new {
                            this.guildEntity = guild.getEntity()
                            this.creatorDid = user.id
                            this.type = AliasType.valueOf(arguments.type)
                            this.search = search
                            this.replace = replace
                        }
                    }.onDuplicate {
                        respondEmbed(
                            ":x: Duplicated Alias",
                            "「$search」を置き換えるエイリアスはすでに存在します。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        log(logger) { guild, user ->
                            "[${guild.name}] Duplicated Alias: @${user.username} attempted to create alias with $arguments but failed due to duplication."
                        }
                        return@action
                    }.unwrap()

                    val row = transaction {
                        entity.getRow()
                    }

                    respondEmbed(
                        ":loudspeaker: Alias Created",
                        "${row.type.displayName}のエイリアスを作成しました"
                    ) {
                        authorOf(user)
                        fieldAliasFrom(row.type, row.search)

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            row.replace
                        }

                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Alias Created: @${user.username} created alias $row"
                    }
                }
            }

            publicSubCommand("update", "エイリアスを更新します。", ::UpdateOptions) {
                action {
                    val aliasEntity = transaction {
                        Entity.findById(arguments.aliasId)
                    } ?: run {
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

                    val oldRow = transaction {
                        aliasEntity.getRow()
                    }

                    val updatedType =
                        arguments.type?.let { typeString -> AliasType.valueOf(typeString) } ?: oldRow.type
                    val updatedSearch = arguments.search ?: oldRow.search
                    val updatedReplace = arguments.replace ?: oldRow.replace

                    if (!validateSoundboardAlias(updatedType, updatedReplace)) return@action

                    transactionResulting(commit = true) {
                        aliasEntity.type = updatedType
                        aliasEntity.search = updatedSearch
                        aliasEntity.replace = updatedReplace
                        aliasEntity.version += 1
                    }.onDuplicate {
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
                        return@action
                    }.unwrap()

                    val newRow = transaction {
                        aliasEntity.getRow()
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
                        Entity.findById(arguments.aliasId)
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
                        val row = aliasEntity.getRow()
                        aliasEntity.delete()
                        row
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
                        Entity.find { Table.guildDid eq guildId }.getRows()
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
