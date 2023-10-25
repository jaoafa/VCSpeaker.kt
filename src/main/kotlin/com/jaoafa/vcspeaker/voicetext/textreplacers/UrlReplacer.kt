package com.jaoafa.vcspeaker.voicetext.textreplacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.models.original.discord.DiscordInvite
import com.jaoafa.vcspeaker.models.response.discord.DiscordGetInviteResponse
import com.jaoafa.vcspeaker.tools.Emoji.removeEmojis
import com.jaoafa.vcspeaker.tools.Twitter
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isThread
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import java.net.MalformedURLException
import java.net.URL

/**
 * URLを置換するクラス
 */
object UrlReplacer : BaseReplacer {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val messageUrlRegex = Regex(
        "^https://.*?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)/(\\d+)\\??(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val channelUrlRegex = Regex(
        "^https://.*?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)\\??(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val eventDirectUrlRegex = Regex(
        "^(?:https?://)?(?:www\\.)?discord(?:app)?\\.com/events/(\\d+)/(\\d+)$",
        RegexOption.IGNORE_CASE
    )
    private val eventInviteUrlRegex = Regex(
        "^(?:https?://)?(?:www\\.)?(?:discord(?:app)?\\.com/invite|discord\\.gg)/(\\w+)\\?event=(\\d+)$",
        RegexOption.IGNORE_CASE
    )
    private val inviteUrlRegex = Regex(
        "^(?:https?://)?(?:www\\.)?(?:discord(?:app)?\\.com/invite|discord\\.gg)/(\\w+)$",
        RegexOption.IGNORE_CASE
    )
    private val tweetUrlRegex = Regex(
        "^https://(?:x|twitter)\\.com/(\\w){1,15}/status/(\\d+)\\??(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val titleRegex = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)
    private val urlRegex = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    private val extensionNameMap = mapOf(
        "jpg" to "JPEGファイル",
        "apng" to "アニメーションPNGファイル",
        "txt" to "テキストファイル",
        "doc" to "Wordファイル",
        "docx" to "Wordファイル",
        "xls" to "Excelファイル",
        "xlsx" to "Excelファイル",
        "ppt" to "PowerPointファイル",
        "pptx" to "PowerPointファイル",
        "exe" to "実行ファイル",
        "7z" to "7Zipファイル",
        "ps1" to "PowerShellスクリプトファイル",
        "bat" to "バッチファイル",
        "cmd" to "コマンドファイル",
        "sh" to "シェルスクリプトファイル",
        "js" to "JavaScriptファイル",
        "htm" to "HTMLファイル",
        "py" to "Pythonファイル",
        "rb" to "Rubyファイル",
        "java" to "Javaファイル",
        "cpp" to "Cプラスプラスファイル",
        "cs" to "C#ファイル",
        "kt" to "Kotlinファイル",
        "rs" to "Rustファイル",
        "yml" to "ヤメルファイル",
        "yaml" to "ヤメルファイル",
        "conf" to "設定ファイル",
        "config" to "設定ファイル",
        "md" to "Markdownファイル",
    )


    override suspend fun replace(text: String, guildId: Snowflake): String {
        suspend fun replaceUrl(vararg replacers: suspend (String, Snowflake) -> String) =
            replacers.fold(text) { replacedText, replacer ->
                replacer(replacedText, guildId)
            }

        return replaceUrl(
            ::replaceMessageUrl,
            ::replaceChannelUrl,
            ::replaceEventDirectUrl,
            ::replaceEventInviteUrl,
            ::replaceTweetUrl,
            ::replaceInviteUrl,
            ::replaceUrlToTitle,
            ::replaceUrl,
        )
    }

    /**
     * チャンネルタイプに応じて、読み上げるチャンネル種別テキストを返します。
     */
    private fun getChannelTypeText(channel: GuildChannel): String {
        return when (channel.type) {
            is ChannelType.GuildText -> "テキストチャンネル"
            is ChannelType.GuildVoice -> "ボイスチャンネル"
            is ChannelType.GuildCategory -> "カテゴリ"
            is ChannelType.GuildNews -> "ニュースチャンネル"
            else -> channel.type.toString() + "チャンネル"
        }
    }

    /**
     * チャンネルIDをもとに、チャンネルを取得します。スレッドの場合は、親チャンネルを取得します。
     */
    private suspend fun getChannel(guild: Guild, channelId: Snowflake): GuildChannel? {
        val channel = guild.getChannelOrNull(channelId) ?: return null

        return if (channel.isThread()) {
            val thread = channel.asChannelOf<ThreadChannel>()
            thread.parent.asChannel()
        } else channel
    }

    /**
     * チャンネルIDをもとに、スレッドを取得します。スレッドでない場合はnullを返します。
     */
    private suspend fun getThread(guild: Guild, channelId: Snowflake): ThreadChannel? {
        val channel = guild.getChannelOrNull(channelId) ?: return null

        return if (channel.isThread()) {
            channel.asChannelOf<ThreadChannel>()
        } else null
    }

    /**
     * 招待リンクをもとに、招待の情報を取得します。
     */
    private suspend fun getInvite(inviteId: String, eventId: Snowflake? = null): DiscordInvite? {
        val url = "https://discord.com/api/invites/$inviteId"
        val response = client.get((url)) {
            parameter("with_counts", true)
            parameter("with_expiration", true)
            if (eventId != null)
                parameter("guild_scheduled_event_id", eventId.value)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val json: DiscordGetInviteResponse = response.body()
                val guild = json.guild
                val channel = json.channel
                val inviter = json.inviter
                val event = json.guildScheduledEvent

                DiscordInvite(
                    json.code,
                    Snowflake(guild.id),
                    guild.name,
                    Snowflake(channel.id),
                    channel.name,
                    Snowflake(inviter.id),
                    inviter.username,
                    event?.id?.let { Snowflake(it) },
                    event?.name
                )
            }

            else -> null
        }
    }

    /**
     * URLをもとに、ページタイトルを取得します。titleタグがない場合はnullを返します。
     */
    private suspend fun getPageTitle(url: String): String? {
        val response = client.get(url)

        val bodyText = String(response.body<ByteArray>())

        return when (response.status) {
            HttpStatusCode.OK -> {
                val matchResult = titleRegex.find(bodyText)
                matchResult?.let {
                    val (title) = matchResult.destructured
                    title
                }
            }

            else -> null
        }
    }

    private fun getExtension(url: String) = try {
        val urlObj = URL(url)
        val path = urlObj.path
        val lastDot = path.lastIndexOf('.')
        if (lastDot == -1) null else path.substring(lastDot + 1)
    } catch (e: MalformedURLException) {
        null
    }

    /**
     * メッセージURLを置換します。
     */
    private suspend fun replaceMessageUrl(text: String, guildId: Snowflake): String {
        val matchResults = messageUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlChannelIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlChannelId = Snowflake(urlChannelIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId) ?: return@fold replacedText.replace(
                matchResult.value,
                "どこかのチャンネルで送信したメッセージのリンク"
            )
            val channel = getChannel(guild, urlChannelId) ?: return@fold replacedText.replace(
                matchResult.value,
                "どこかのチャンネルで送信したメッセージのリンク"
            )
            val channelType = getChannelTypeText(channel)
            val thread = getThread(guild, urlChannelId)

            val replaceTo = if (thread != null) {
                "$channelType「${thread.name}」のスレッド「${thread.parent.asChannel().name}」で送信したメッセージのリンク"
            } else {
                "$channelType「${channel.name}」で送信したメッセージのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * チャンネルURLを置換します。
     */
    private suspend fun replaceChannelUrl(text: String, guildId: Snowflake): String {
        val matchResults = channelUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlChannelIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlChannelId = Snowflake(urlChannelIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId) ?: return@fold replacedText.replace(
                matchResult.value,
                "どこかのチャンネルで送信したメッセージのリンク"
            )
            val channel = getChannel(guild, urlChannelId) ?: return@fold replacedText.replace(
                matchResult.value,
                "どこかのチャンネルで送信したメッセージのリンク"
            )
            val channelType = getChannelTypeText(channel)
            val thread = getThread(guild, urlChannelId)

            val replaceTo = if (thread != null) {
                "$channelType「${thread.parent.asChannel().name}」のスレッド「${thread.name}」へのリンク"
            } else {
                "$channelType「${channel.name}」へのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * イベントへの直接URLを置換します。
     */
    private suspend fun replaceEventDirectUrl(text: String, guildId: Snowflake): String {
        val matchResults = eventDirectUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlEventIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlEventId = Snowflake(urlEventIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId) ?: return@fold replacedText.replace(
                matchResult.value,
                "どこかのサーバのイベントへのリンク"
            )

            val event = guild.scheduledEvents.firstOrNull { it.id == urlEventId }
                ?: return@fold replacedText.replace(
                    matchResult.value,
                    "サーバ「${guild.name}」のイベントへのリンク"
                )

            val replaceTo = if (guild.id == guildId) {
                "イベント「${event.name}」へのリンク"
            } else {
                "サーバ「${guild.name}」のイベント「${event.name}」へのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * イベントへの招待URLを置換します。
     */
    private suspend fun replaceEventInviteUrl(text: String, guildId: Snowflake): String {
        val matchResults = eventInviteUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (inviteCode, urlEventIdRaw) = matchResult.destructured
            val urlEventId = Snowflake(urlEventIdRaw)

            val invite = getInvite(inviteCode, urlEventId)
                ?: return@fold replacedText.replace(
                    matchResult.value,
                    "どこかのサーバのイベントへのリンク"
                )

            val replaceTo = if (invite.guildId == guildId) {
                "イベント「${invite.eventName}」へのリンク"
            } else {
                "サーバ「${invite.guildName}」のイベント「${invite.eventName}」へのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    private suspend fun replaceInviteUrl(text: String, guildId: Snowflake): String {
        val matchResults = inviteUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (inviteCode) = matchResult.destructured

            // URLに含まれているIDをもとに、エンティティを取得する
            val invite = getInvite(inviteCode)
                ?: return@fold replacedText.replace(
                    matchResult.value,
                    "どこかのサーバへの招待リンク"
                )

            val replaceTo = if (invite.guildId == guildId) {
                "チャンネル「${invite.channelName}」への招待リンク"
            } else {
                "サーバ「${invite.guildName}」のチャンネル「${invite.channelName}」への招待リンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * ツイートURLを置換します。
     */
    private suspend fun replaceTweetUrl(text: String, guildId: Snowflake): String {
        val matchResults = tweetUrlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val (userName, tweetId) = matchResult.destructured

            val tweet = Twitter.getTweet(userName, tweetId) ?: return@fold replacedText.replace(
                matchResult.value,
                "ユーザー「$userName」のツイートへのリンク"
            )

            val tweetContent = tweet.plainText.substring(
                0,
                70.coerceAtMost(tweet.plainText.length)
            ) + if (tweet.plainText.length > 70) " 以下略" else ""
            val replaceTo = "${tweet.authorName.removeEmojis()}のツイート「$tweetContent」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * URLをページのタイトルに置換します。
     */
    private suspend fun replaceUrlToTitle(text: String, guildId: Snowflake): String {
        val matchResults = urlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val url = matchResult.value

            val title = getPageTitle(url) ?: return@fold replacedText
            val shortTitle = title.substring(
                0,
                30.coerceAtMost(title.length)
            ) + if (title.length > 30) " 以下略" else ""

            val replaceTo = "Webページ「$shortTitle」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }

    /**
     * URLをもとに、拡張子を取得し置き換えます。拡張子がない場合は、「Webページのリンク」と置き換えます。
     */
    private fun replaceUrl(text: String, guildId: Snowflake): String {
        val matchResults = urlRegex.findAll(text)

        val replacedText = matchResults.fold(text) { replacedText, matchResult ->
            val url = matchResult.value

            val extension = getExtension(url) ?: return@fold replacedText.replace(
                matchResult.value,
                "Webページのリンク"
            )

            if (extensionNameMap.containsKey(extension)) {
                return@fold replacedText.replace(
                    matchResult.value,
                    "${extensionNameMap[extension]}へのリンク"
                )
            }

            val replaceTo = "${extension}ファイルへのリンク"
            replacedText.replace(matchResult.value, replaceTo)
        }

        return replacedText
    }
}