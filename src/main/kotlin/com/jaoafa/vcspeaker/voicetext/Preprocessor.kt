package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.store.AliasData
import com.jaoafa.vcspeaker.store.AliasStore
import com.jaoafa.vcspeaker.store.AliasType
import com.jaoafa.vcspeaker.store.IgnoreStore
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel

// ignore -> emoji -> regex -> text

/**
 * The Preprocessor class provides methods for processing text by applying various replacements based on the guild ID.
 */
object Preprocessor {
    /**
     * Processes the given text by applying various replacements based on the guild ID.
     * If the text should be ignored based on the guild ID, returns null.
     *
     * @param guildId The ID of the guild for which the text is being processed.
     * @param text The text to be processed.
     * @return The processed text, or null if the text should be ignored.
     */
    suspend fun processText(guildId: Snowflake, text: String): String? {
        if (shouldIgnore(text, guildId)) return null

        var processedText = text

        suspend fun replace(vararg replacers: suspend (String, Snowflake) -> String) {
            for (replacer in replacers)
                processedText = replacer(processedText, guildId)
        }

        replace(
            ::replaceEmoji,
            ::replaceRegex,
            ::replaceAlias,
            ::replaceChannelMention,
            ::replaceRoleMention,
            ::replaceUserMention,
            ::replaceMessageMention
        )

        return processedText
    }

    /**
     * Determines whether the specified text should be ignored for the given guild ID.
     *
     * @param text The text to be checked.
     * @param guildId The unique identifier of the guild.
     * @return true if the text should be ignored, false otherwise.
     */
    private fun shouldIgnore(text: String, guildId: Snowflake) =
        IgnoreStore.filter(guildId).any {
            text.contains(it.text)
        }

    /**
     * Replaces emojis in the given text with their corresponding aliases.
     *
     * @param text The text containing emojis to be replaced.
     * @param guildId The ID of the guild the text belongs to.
     * @return The text with emojis replaced by their respective aliases.
     */
    private fun replaceEmoji(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Emoji) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    /**
     * Replaces matching occurrences of the regular expression alias with the corresponding replacement text in the given text.
     *
     * @param text The original text to perform the replacement on.
     * @param guildId The ID of the guild for which the replacement is being performed.
     * @return The text with the regular expression aliases replaced.
     */
    private fun replaceRegex(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Regex) { alias, replacedText ->
            replacedText.replace(Regex(alias.from), alias.to)
        }

    /**
     * Replaces aliases in the given text with their corresponding replacements.
     *
     * @param text The text to replace aliases in.
     * @param guildId The ID of the guild to retrieve aliases from.
     * @return The text with aliases replaced.
     */
    private fun replaceAlias(text: String, guildId: Snowflake) =
        replaceText(text, guildId, AliasType.Text) { alias, replacedText ->
            replacedText.replace(alias.from, alias.to)
        }

    /**
     * Replaces text in a given string using aliases specific to a guild and type.
     *
     * @param text The original string in which the text will be replaced.
     * @param guildId The unique identifier of the guild.
     * @param type The type of aliases to consider for replacement.
     * @param transform A higher-order function that transforms the replaced text using an alias and the original string.
     *                  The function takes an AliasData object and the original text as parameters, and returns the transformed text.
     * @return The string with the replaced text.
     */
    private fun replaceText(
        text: String,
        guildId: Snowflake,
        type: AliasType,
        transform: (AliasData, String) -> String
    ): String {
        val aliases = AliasStore.filter(guildId).filter { it.type == type }

        var replacedText = text

        for (alias in aliases)
            replacedText = transform(alias, replacedText)

        return replacedText
    }


    /**
     * Replaces channel mentions in the given [text] with their corresponding names.
     *
     * @param text The text to replace channel mentions in.
     * @param guildId The ID of the guild to search for channels.
     * @return The text with channel mentions replaced with their names.
     */
    private suspend fun replaceChannelMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<#(\\d+)>")) { kord, id ->
            kord.getChannel(id)?.data?.name?.value ?: "不明なチャンネル"
        }

    /**
     * Replaces role mentions in the given [text] with their respective names.
     *
     * @param text The text containing the role mentions.
     * @param guildId The ID of the guild to retrieve the roles from.
     * @return The modified text with role mentions replaced by their names.
     */
    private suspend fun replaceRoleMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@&(\\d+)>")) { kord, id ->
            kord.getGuildOrNull(guildId)?.getRole(id)?.data?.name ?: "不明なロール"
        }

    /**
     * Replaces user mentions in the given text with the corresponding user information.
     *
     * @param text The text to replace user mentions in.
     * @param guildId The ID of the guild where the user mention belongs to.
     * @return The text with user mentions replaced or "不明なユーザー" if the user information is not found.
     */
    private suspend fun replaceUserMention(text: String, guildId: Snowflake) =
        replaceMentionable(text, Regex("<@!?(\\d+)>")) { kord, id ->
            val displayName = kord.getGuildOrNull(guildId)?.getMember(id)?.displayName
            displayName ?: "不明なユーザー"
        }

    /**
     * Replaces message mentions in the given text with a formatted text indicating the source message.
     *
     * @param text The input text to replace message mentions in.
     * @param guildId The ID of the guild where the messages are located.
     * @return The text with replaced message mentions.
     */
    private suspend fun replaceMessageMention(text: String, guildId: Snowflake): String {
        val matches = Regex("https://(\\w+\\.)*discord.com/channels/(\\d+)/(\\d+)/(\\d+)").findAll(text)

        var replacedText = text

        for (match in matches) {
            val channelId = Snowflake(match.groupValues[3])
            val messageId = Snowflake(match.groupValues[4])

            val channel = VCSpeaker.instance.kordRef.getChannelOf<TextChannel>(channelId)
            val message = channel?.getMessageOrNull(messageId) ?: continue

            val read = "${message.author?.username ?: "システム"} が ${channel.name} で送信したメッセージへのリンク"

            replacedText = replacedText.replace(match.value, read)
        }

        return replacedText
    }

    /**
     * Replaces mentionable placeholders in the given text with the corresponding names.
     *
     * @param text The text to perform replacements on.
     * @param regex The regular expression used to find mentionable placeholders in the text.
     * @param nameSupplier The supplier function that retrieves the name for a given Snowflake ID.
     * @return The text with mentionable placeholders replaced by names.
     */
    private suspend fun replaceMentionable(
        text: String,
        regex: Regex,
        nameSupplier: suspend (Kord, Snowflake) -> String
    ): String {
        val matches = regex.findAll(text)

        var replacedText = text

        for (match in matches) {
            val id = Snowflake(match.groupValues[1]) // 0 is for whole match
            val name = nameSupplier(VCSpeaker.instance.kordRef, id)

            replacedText = replacedText.replace(match.value, name)
        }

        return replacedText
    }
}