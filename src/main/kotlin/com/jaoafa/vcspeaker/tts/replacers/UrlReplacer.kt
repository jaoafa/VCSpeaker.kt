package com.jaoafa.vcspeaker.tts.replacers

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.models.original.discord.DiscordInvite
import com.jaoafa.vcspeaker.models.response.discord.DiscordGetInviteResponse
import com.jaoafa.vcspeaker.tools.Emoji.removeEmojis
import com.jaoafa.vcspeaker.tools.Steam
import com.jaoafa.vcspeaker.tools.Twitter
import com.jaoafa.vcspeaker.tools.YouTube
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.isThread
import com.jaoafa.vcspeaker.tts.TextProcessor.substringByCodePoints
import com.jaoafa.vcspeaker.tts.Token
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
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import java.net.MalformedURLException
import kotlin.text.String

/**
 * URLを置換するクラス
 */
object UrlReplacer : BaseReplacer {
    override val priority = ReplacerPriority.High

    override suspend fun replace(tokens: MutableList<Token>, guildId: Snowflake): MutableList<Token> {
        suspend fun replaceUrl(vararg replacers: suspend (String, Snowflake) -> String) =
            replacers.fold(tokens.joinToString("") { it.text }) { replacedText, replacer ->
                replacer(replacedText, guildId)
            }

        return mutableListOf(
            Token(
                replaceUrl(
                    ::replaceMessageUrl,
                    ::replaceChannelUrl,
                    ::replaceEventDirectUrl,
                    ::replaceEventInviteUrl,
                    ::replaceTweetUrl,
                    ::replaceInviteUrl,
                    ::replaceSteamAppUrl,
                    ::replaceYouTubeUrl,
                    ::replaceYouTubePlaylistUrl,
                    ::replaceGoogleSearchUrl,
                    ::replaceUrlToTitle,
                    ::replaceUrl,
                )
            )
        )
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    /**
     * DiscordのメッセージURLを表す正規表現
     *
     * 例: https://discord.com/channels/123456789012345678/123456789012345678/123456789012345678
     * 例: https://discordapp.com/channels/123456789012345678/123456789012345678/123456789012345678
     * 例: https://discord.com/channels/123456789012345678/123456789012345678/123456789012345678?query=example
     * 例: https://discordapp.com/channels/123456789012345678/123456789012345678/123456789012345678?query=example
     */
    private val messageUrlRegex = Regex(
        "https://.*?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)/(\\d+)\\??(.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * DiscordのチャンネルURLを表す正規表現
     *
     * 例: https://discord.com/channels/123456789012345678/123456789012345678
     * 例: https://discordapp.com/channels/123456789012345678/123456789012345678
     * 例: https://discord.com/channels/123456789012345678/123456789012345678?query=example
     * 例: https://discordapp.com/channels/123456789012345678/123456789012345678?query=example
     */
    private val channelUrlRegex = Regex(
        "https://.*?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)\\??(.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Discordのイベントへの直接URLを表す正規表現
     *
     * 例: https://discord.com/events/123456789012345678/123456789012345678
     * 例: https://discordapp.com/events/123456789012345678/123456789012345678
     * 例: https://discord.com/events/123456789012345678/123456789012345678?query=example
     * 例: https://discordapp.com/events/123456789012345678/123456789012345678?query=example
     */
    private val eventDirectUrlRegex = Regex(
        "(?:https?://)?(?:www\\.)?discord(?:app)?\\.com/events/(\\d+)/(\\d+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Discordのイベントへの招待URLを表す正規表現
     *
     * 例: https://discord.com/invite/abcdef?event=123456789012345678
     * 例: https://discordapp.com/invite/abcdef?event=123456789012345678
     * 例: https://discord.com/invite/abcdef?event=123456789012345678&query=example
     * 例: https://discordapp.com/invite/abcdef?event=123456789012345678&query=example
     * 例: https://discord.gg/abcdef?event=123456789012345678
     * 例: https://discord.gg/abcdef?event=123456789012345678&query=example
     * 例: discord.com/invite/abcdef?event=123456789012345678
     * 例: discordapp.com/invite/abcdef?event=123456789012345678
     * 例: discord.gg/abcdef?event=123456789012345678
     * 例: discord.gg/abcdef?event=123456789012345678&query=example
     */
    private val eventInviteUrlRegex = Regex(
        "(?:https?://)?(?:www\\.)?(?:discord(?:app)?\\.com/invite|discord\\.gg)/(\\w+)\\?event=(\\d+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Discordの招待URLを表す正規表現
     *
     * 例: https://discord.com/invite/abcdef
     * 例: https://discordapp.com/invite/abcdef
     * 例: https://discord.com/invite/abcdef?query=example
     * 例: https://discordapp.com/invite/abcdef?query=example
     * 例: https://discord.gg/abcdef
     * 例: https://discord.gg/abcdef?query=example
     * 例: discord.com/invite/abcdef
     */
    private val inviteUrlRegex = Regex(
        "(?:https?://)?(?:www\\.)?(?:discord(?:app)?\\.com/invite|discord\\.gg)/(\\w+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * ツイートURLを表す正規表現
     *
     * 例: https://twitter.com/username/status/123456789012345678
     * 例: https://twitter.com/username/status/123456789012345678?query=example
     * 例: https://twitter.com/username/status/123456789012345678/
     * 例: https://twitter.com/username/status/123456789012345678/?query=example
     * 例: https://x.com/username/status/123456789012345678
     * 例: https://x.com/username/status/123456789012345678?query=example
     * 例: https://x.com/username/status/123456789012345678/
     * 例: https://x.com/username/status/123456789012345678/?query=example
     */
    private val tweetUrlRegex = Regex(
        "https://(?:x|twitter)\\.com/(\\w){1,15}/status/(\\d+)\\??(.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * SteamアイテムへのURLを表す正規表現
     *
     * 例: https://store.steampowered.com/app/1234567890
     * 例: https://store.steampowered.com/app/1234567890?query=example
     */
    private val steamAppUrlRegex = Regex(
        "https://store\\.steampowered\\.com/app/(\\d+)(.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * YouTubeのURLを表す正規表現 (動画、ライブ、ショートに対応。no-cookieも対応)
     *
     * 例: https://www.youtube.com/watch?v=abcdefg
     */
    private val youtubeUrlRegex = Regex(
        "(?:https?:)?(?://)?(?:youtu\\.be/|(?:www\\.|m\\.)?(?:youtube\\.com|youtube-nocookie\\.com)/(watch|v|e|embed|shorts|live)(?:\\.php)?(?:\\?.*v=|/))([a-zA-Z0-9_-]{7,15})(?:[?&][a-zA-Z0-9_-]+=[a-zA-Z0-9_-]+)*(?:[&/#].*)?",
        RegexOption.IGNORE_CASE
    )

    /**
     * YouTubeのプレイリストURLを表す正規表現
     *
     * 例: https://www.youtube.com/playlist?list=abcdefg
     */
    private val youtubePlaylistUrlRegex = Regex(
        "(?:https?:)?(?://)?(?:www\\.|m\\.)?youtube\\.com/playlist\\?list=([a-zA-Z0-9_-]+)(?:[?&][a-zA-Z0-9_-]+=[a-zA-Z0-9_-]+)*(?:[&/#].*)?",
        RegexOption.IGNORE_CASE
    )

    /**
     * タイトル要素を取得するための正規表現
     */
    private val titleRegex = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)

    /**
     * URLを表す正規表現
     */
    private val urlRegex = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    /**
     * 拡張子名とその拡張子に対応する呼び名のマップ
     */
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

    /**
     * チャンネルタイプに応じて、読み上げるチャンネル種別テキストを返します。
     */
    private fun getChannelTypeText(channel: GuildChannel) = when (channel.type) {
        is ChannelType.GuildText -> "テキストチャンネル"
        is ChannelType.GuildVoice -> "ボイスチャンネル"
        is ChannelType.GuildCategory -> "カテゴリ"
        is ChannelType.GuildNews -> "ニュースチャンネル"
        else -> channel.type.toString() + "チャンネル"
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
        var byteArray = ByteArray(0)

        client.prepareGet(url).execute {
            val channel: ByteReadChannel = it.body()
            if (!channel.isClosedForRead) {
                val packet = channel.readRemaining(1024 * 2)

                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    byteArray += bytes
                }
            }
        }

        val bodyText = String(byteArray)

        val matchResult = titleRegex.find(bodyText)

        return matchResult?.let {
            val (title) = matchResult.destructured
            title
        }
    }

    /**
     * URLから拡張子を取得します。
     */
    private fun getExtension(url: String) = try {
        val path = Url(url).pathSegments.last()
        val dotPath = path.split(".")
        if (dotPath.size > 1) dotPath.last() else null
    } catch (e: MalformedURLException) {
        null
    }

    /**
     * メッセージURLを置換します。
     */
    private suspend fun replaceMessageUrl(text: String, guildId: Snowflake) =
        messageUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlChannelIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlChannelId = Snowflake(urlChannelIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId)
            val channel = guild?.let { getChannel(it, urlChannelId) }

            if (guild == null || channel == null)
                return@replaceAll replacedText.replace(
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

    /**
     * チャンネルURLを置換します。
     */
    private suspend fun replaceChannelUrl(text: String, guildId: Snowflake) =
        channelUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlChannelIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlChannelId = Snowflake(urlChannelIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId)
            val channel = guild?.let { getChannel(it, urlChannelId) }

            if (guild == null || channel == null)
                return@replaceAll replacedText.replace(matchResult.value, "どこかのチャンネルへのリンク")

            val channelType = getChannelTypeText(channel)
            val thread = getThread(guild, urlChannelId)

            val replaceTo = if (thread != null) {
                "$channelType「${thread.parent.asChannel().name}」のスレッド「${thread.name}」へのリンク"
            } else {
                "$channelType「${channel.name}」へのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * イベントへの直接URLを置換します。
     */
    private suspend fun replaceEventDirectUrl(text: String, guildId: Snowflake) =
        eventDirectUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (urlGuildIdRaw, urlEventIdRaw) = matchResult.destructured
            val urlGuildId = Snowflake(urlGuildIdRaw)
            val urlEventId = Snowflake(urlEventIdRaw)

            // URLに含まれているIDをもとに、エンティティを取得する
            val guild = VCSpeaker.kord.getGuildOrNull(urlGuildId) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "どこかのサーバのイベントへのリンク"
            )

            val event = guild.scheduledEvents.firstOrNull { it.id == urlEventId }
                ?: return@replaceAll replacedText.replace(
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

    /**
     * イベントへの招待URLを置換します。
     */
    private suspend fun replaceEventInviteUrl(text: String, guildId: Snowflake) =
        eventInviteUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (inviteCode, urlEventIdRaw) = matchResult.destructured
            val urlEventId = Snowflake(urlEventIdRaw)

            val invite = getInvite(inviteCode, urlEventId)
                ?: return@replaceAll replacedText.replace(
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

    private suspend fun replaceInviteUrl(text: String, guildId: Snowflake) =
        inviteUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (inviteCode) = matchResult.destructured

            // URLに含まれているIDをもとに、エンティティを取得する
            val invite = getInvite(inviteCode)
                ?: return@replaceAll replacedText.replace(
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

    /**
     * ツイートURLを置換します。
     */
    private suspend fun replaceTweetUrl(text: String, guildId: Snowflake) =
        tweetUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (userName, tweetId) = matchResult.destructured

            val tweet = Twitter.getTweet(userName, tweetId) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "ユーザー「$userName」のツイートへのリンク"
            )

            val tweetContent = tweet.readText.substringByCodePoints(
                0,
                70.coerceAtMost(tweet.readText.length)
            ) + if (tweet.readText.length > 70) " 以下略" else ""
            val replaceTo = "${tweet.authorName.removeEmojis()}のツイート「$tweetContent」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * SteamアイテムへのURLを置換します。
     */
    private suspend fun replaceSteamAppUrl(text: String, guildId: Snowflake) =
        steamAppUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (appId) = matchResult.destructured

            val item = Steam.getAppDetail(appId) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "Steamアイテムへのリンク"
            )

            val replaceTo = "Steamアイテム「${item.data.name}」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * YouTubeのURLを置換します。動画、ショート、ライブなどに対応しています。プレイリストは {@link #replaceYouTubePlaylistUrl} で置換します。
     */
    private suspend fun replaceYouTubeUrl(text: String, guildId: Snowflake) =
        youtubeUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (videoType, videoId) = matchResult.destructured
            val video = YouTube.getVideo(videoId) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "YouTube動画へのリンク"
            )

            // 動画タイトルが20文字を超える場合は、20文字に短縮して「以下略」を付ける
            val videoTitle = video.title.shorten(20)
            // 投稿者名が15文字を超える場合は、15文字に短縮して「以下略」を付ける
            val authorName = video.authorName.shorten(15)

            // URLからアイテムの種別を断定できる場合は、それに応じたテンプレートを使用する
            val replaceTo = when (videoType) {
                "shorts" -> "YouTubeの「${authorName}」によるショート「${videoTitle}」へのリンク"
                "live" -> "YouTubeの「${authorName}」による配信「${videoTitle}」へのリンク"
                else -> "YouTubeの「${authorName}」による動画「${videoTitle}」へのリンク"
            }

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * YouTubeのプレイリストURLを置換します。
     */
    private suspend fun replaceYouTubePlaylistUrl(text: String, guildId: Snowflake) =
        youtubePlaylistUrlRegex.replaceAll(text) { replacedText, matchResult ->
            val (playlistId) = matchResult.destructured

            val playlist = YouTube.getPlaylist(playlistId) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "YouTubeプレイリストへのリンク"
            )

            val replaceTo = "YouTubeの「${playlist.authorName}」によるプレイリスト「${playlist.title}」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * Google検索のURLを置換します。
     */
    private suspend fun replaceGoogleSearchUrl(text: String, guildId: Snowflake) =
        urlRegex.replaceAll(text) { replacedText, matchResult ->
            val url = matchResult.value

            val expectedUrlStarts = "https://www.google.com/search"
            if (!url.startsWith(expectedUrlStarts)) return@replaceAll replacedText

            val urlParams = Url(url).parameters
            val query = urlParams["q"] ?: return@replaceAll replacedText

            val replaceTo = "Google検索「$query」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * URLをページのタイトルに置換します。
     */
    private suspend fun replaceUrlToTitle(text: String, guildId: Snowflake) =
        urlRegex.replaceAll(text) { replacedText, matchResult ->
            val url = matchResult.value

            val title = getPageTitle(url)?.shorten(30) ?: return@replaceAll replacedText

            val replaceTo = "Webページ「$title」へのリンク"

            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * URLをもとに、拡張子を取得し置き換えます。拡張子がない場合は、「Webページのリンク」と置き換えます。
     */
    private suspend fun replaceUrl(text: String, guildId: Snowflake) =
        urlRegex.replaceAll(text) { replacedText, matchResult ->
            val url = matchResult.value

            val extension = getExtension(url) ?: return@replaceAll replacedText.replace(
                matchResult.value,
                "Webページのリンク"
            )

            if (extensionNameMap.containsKey(extension)) {
                return@replaceAll replacedText.replace(
                    matchResult.value,
                    "${extensionNameMap[extension]}へのリンク"
                )
            }

            val replaceTo = "${extension}ファイルへのリンク"
            replacedText.replace(matchResult.value, replaceTo)
        }

    /**
     * 正規表現にマッチした文字列を置換します。
     */
    private suspend fun Regex.replaceAll(text: String, replacer: suspend (String, MatchResult) -> String): String {
        val matchResults = this.findAll(text)
        return matchResults.fold(text) { replacedText, matchResult ->
            replacer(replacedText, matchResult)
        }
    }

    /**
     * 文字列を指定した長さに短縮します。短縮後、末尾に「以下略」を付けます。
     */
    private fun String.shorten(length: Int) = if (this.length > length) substringByCodePoints(0, length) + " 以下略" else this
}