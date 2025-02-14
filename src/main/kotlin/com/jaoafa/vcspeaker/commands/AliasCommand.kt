package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.Alias
import com.jaoafa.vcspeaker.features.Alias.fieldAliasFrom
import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasStore
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import dev.kordex.core.annotations.AlwaysPublicResponse
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.capitalizeWords
import io.github.oshai.kotlinlogging.KotlinLogging

class AliasCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    inner class CreateOptions : Options() {
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

    inner class UpdateOptions : Options() {
        val alias by string {
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

    inner class DeleteOptions : Options() {
        val search by string {
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
                    val type = AliasType.valueOf(arguments.type)
                    val search = arguments.search
                    val replace = arguments.replace

                    val duplicate = AliasStore.find(guild!!.id, search)
                    val isUpdate = duplicate != null
                    val oldReplace = duplicate?.replace

                    if (isUpdate) AliasStore.remove(duplicate!!) // checked

                    AliasStore.create(AliasData(guild!!.id, user.id, type, search, replace))

                    respondEmbed(
                        ":loudspeaker: Alias ${if (isUpdate) "Updated" else "Created"}",
                        "${type.displayName}のエイリアスを${if (isUpdate) "更新" else "作成"}しました"
                    ) {
                        authorOf(user)

                        fieldAliasFrom(type, search)

                        field(":arrows_counterclockwise: 置き換える文字列", true) {
                            if (isUpdate) "$oldReplace → **$replace**" else replace
                        }

                        successColor()
                    }

                    val verb = if (isUpdate) "updated" else "created"
                    val typeName = type.name.lowercase()

                    log(logger) { guild, user ->
                        "[${guild.name}] Alias ${verb.capitalizeWords()}: @${user.username} $verb $typeName alias that replaces \"$search\" to \"$replace\"" +
                                if (isUpdate) " (updated from \"$oldReplace\")" else ""
                    }
                }
            }

            publicSubCommand("update", "エイリアスを更新します。", ::UpdateOptions) {
                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.alias)
                    if (aliasData != null) {
                        val (_, _, type, search, replace) = aliasData

                        val updatedType = arguments.type?.let { typeString -> AliasType.valueOf(typeString) } ?: type
                        val updatedSearch = arguments.search ?: search
                        val updatedReplace = arguments.replace ?: replace

                        AliasStore.remove(aliasData)
                        AliasStore.create(
                            aliasData.copy(
                                userId = user.id,
                                type = updatedType,
                                search = updatedSearch,
                                replace = updatedReplace
                            )
                        )

                        respondEmbed(
                            ":repeat: Alias Updated",
                            "エイリアスを更新しました。"
                        ) {
                            authorOf(user)

                            fun searchDisplay(type: AliasType, search: String) = when (type) {
                                AliasType.Text -> search
                                AliasType.Regex -> "`$search`"
                                AliasType.Emoji -> "$search `$search`"
                            }

                            field("${updatedType.emoji} ${updatedType.displayName}", true) {
                                searchDisplay(type, search) + if (replace != updatedReplace)
                                    " → **${searchDisplay(updatedType, updatedSearch)}**"
                                else ""
                            }

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                if (replace != updatedReplace) "「$replace」→「**$updatedReplace**」" else "「$replace」"
                            }

                            successColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Updated: @${user.username} updated $type alias that replaces \"$search\" to \"$updatedReplace\" (updated from \"$replace\")"
                        }
                    } else {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "置き換え条件が「${arguments.alias}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Not Found: @${user.username} searched for alias contains \"${arguments.alias}\" but not found"
                        }
                    }
                }
            }

            publicSubCommand("delete", "エイリアスを削除します。", ::DeleteOptions) {
                action {
                    val aliasData = AliasStore.find(guild!!.id, arguments.search)

                    if (aliasData != null) {
                        AliasStore.remove(aliasData)

                        val (_, _, type, search, replace) = aliasData

                        respondEmbed(
                            ":wastebasket: Alias Deleted",
                            "${type.displayName}のエイリアスを削除しました。"
                        ) {
                            authorOf(user)

                            fieldAliasFrom(type, search)

                            field(":arrows_counterclockwise: 置き換える文字列", true) {
                                replace
                            }

                            successColor()
                        }

                        val username = user.asUser().username

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Deleted: @${user.username} deleted $type alias that replaces \"$search\" to \"$replace\""
                        }
                    } else {
                        respondEmbed(
                            ":question: Alias Not Found",
                            "置き換え条件が「${arguments.search}」のエイリアスは見つかりませんでした。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }

                        log(logger) { guild, user ->
                            "[${guild.name}] Alias Not Found: @${user.username} searched for alias contains \"${arguments.search}\" but not found"
                        }
                    }
                }
            }

            publicSubCommand("list", "エイリアスの一覧を表示します。") {
                action {
                    val aliases = AliasStore.filter(guild!!.id)

                    if (aliases.isEmpty()) {
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
                        for (chunkedAliases in aliases.chunked(10)) {
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
}