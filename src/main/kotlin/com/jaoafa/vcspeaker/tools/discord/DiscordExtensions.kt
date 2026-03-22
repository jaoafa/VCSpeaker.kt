package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.PublicSlashCommandContext
import dev.kordex.core.commands.chat.ChatCommandContext
import dev.kordex.core.types.PublicInteractionContext
import dev.kordex.core.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

typealias EmbedBuilderLambdaSuspend = suspend EmbedBuilder.() -> Unit
typealias EmbedBuilderLambda = EmbedBuilder.() -> Unit

/**
 * Discord 関連の拡張関数をまとめたオブジェクト。
 */
object DiscordExtensions {
    private val logger = KotlinLogging.logger { }

    fun Guild.getSettings() = GuildStore.getOrDefault(this.id)

    /**
     * 自動入退室が有効化されているかどうか。
     */
    fun Guild.autoJoinEnabled() = GuildStore.getOrDefault(this.id).autoJoin

    /**
     * AFK チャンネルかどうか。
     */
    suspend fun BaseVoiceChannelBehavior.isAfk() = this.getGuild().afkChannel == this

    /**
     * Embed の Author を [user] に設定します。
     */
    fun EmbedBuilder.authorOf(user: User) {
        author {
            name = user.username
            icon = user.avatar?.cdnUrl?.toUrl()
        }
    }

    /**
     * Embed の Author を [user] に設定します。
     */
    suspend fun EmbedBuilder.authorOf(user: UserBehavior) = authorOf(user.asUser())

    suspend fun EmbedBuilder.guildParameterOf(entity: GuildEntity) {
        val values = transaction { entity.readValues }
        field {
            name = ":hash: 読み上げチャンネル"
            value = values[GuildTable.channelDid]?.asChannelOf<TextChannel>()?.mention ?: "未設定"
            inline = true
        }
        field {
            name = ":symbols: プレフィックス"
            value = values[GuildTable.prefix]?.let { "`$it`" } ?: "未設定"
            inline = true
        }

        val voice = transaction { entity.speakerVoiceEntity }

        field {
            name = ":grinning: 話者"
            value = voice.speaker.speakerName
            inline = true
        }

        val emotion = voice.emotion
        field {
            name = "${emotion?.emoji ?: ":neutral_face:"} 感情"
            value = emotion?.emotionName ?: "未設定"
            inline = true
        }
        field {
            name = ":signal_strength: 感情レベル"
            value = voice.emotionLevel?.let { "`Level $it`" } ?: "未設定"
            inline = true
        }
        field {
            name = ":arrow_up_down: ピッチ"
            value = voice.pitch.let { "`$it%`" }
            inline = true
        }
        field {
            name = ":fast_forward: 速度"
            value = voice.speed.let { "`$it%`" }
            inline = true
        }
        field {
            name = ":loud_sound: 音量"
            value = voice.volume.let { "`$it%`" }
            inline = true
        }
        field {
            name = ":inbox_tray: 自動入退室"
            value = if (values[GuildTable.autoJoin]) "有効" else "無効"
            inline = true
        }
    }

    object EmbedColors {
        val success = Color(0xa6e3a1)
        val error = Color(0xf38ba8)
        val warning = Color(0xfab387)
        val info = Color(0xf5e0dc)
    }

    /**
     * Embed の色を [EmbedColors.success] に設定します。
     */
    fun EmbedBuilder.successColor() {
        color = EmbedColors.success
    }

    /**
     * Embed の色を [EmbedColors.error] に設定します。
     */
    fun EmbedBuilder.errorColor() {
        color = EmbedColors.error
    }

    /**
     * Embed の色を [EmbedColors.warning] に設定します。
     */
    fun EmbedBuilder.warningColor() {
        color = EmbedColors.warning
    }

    /**
     * Embed の色を [EmbedColors.info] に設定します。
     */
    fun EmbedBuilder.infoColor() {
        color = EmbedColors.info
    }

    /**
     * Interaction に [content] を返信します。
     */
    suspend fun PublicInteractionContext.respond(
        content: String
    ) = this.respond {
        this.content = content
    }

    /**
     * Embed を Interaction に返信します。
     *
     * @param title タイトル
     * @param description 説明
     * @param builder 適用する EmbedBuilder
     */
    suspend fun PublicSlashCommandContext<*, *>.respondEmbed(
        title: String,
        description: String? = null,
        builder: EmbedBuilderLambdaSuspend = {}
    ) = this.respond {
        embed(title, description, builder)
    }

    /**
     * Embed を Interaction に返信します。
     *
     * @param builder 適用する EmbedBuilder
     * @param override builder を上書きする EmbedBuilder
     */
    suspend fun PublicSlashCommandContext<*, *>.respondEmbedOf(
        builder: EmbedBuilderLambdaSuspend,
        override: EmbedBuilderLambdaSuspend? = null
    ) = this.respond {
        embed {
            apply { builder() }
            if (override != null) {
                apply { override() }
            }
        }
    }

    /**
     * Embed を作成します。
     *
     * @param title タイトル
     * @param description 説明
     * @param builder 適用する EmbedBuilder
     */
    suspend fun FollowupMessageCreateBuilder.embed(
        title: String,
        description: String? = null,
        builder: EmbedBuilderLambdaSuspend = {}
    ) = this.embed {
        this.title = title
        this.description = description
        apply { builder() }
    }

    /**
     * [Snowflake] に対応するチャンネルを [T] として取得します。
     */
    suspend inline fun <reified T : Channel> Snowflake.asChannelOf() = VCSpeaker.kord.getChannelOf<T>(this)

    /**
     * [VoiceChannel] か [member] が現在いるチャンネル、または null を、次の優先度に従って返します。
     *
     * 優先度 : [VoiceChannel] > [member] > null
     *
     * @param member 対象のメンバー
     * @param onFailure エラーメッセージの返信処理 (it にはエラーメッセージが入ります)
     */
    suspend fun Channel?.orFallbackTo(member: MemberBehavior, onFailure: suspend (String) -> Unit) =
        this?.asChannelOf<VoiceChannel>() ?: member.getVoiceStateOrNull()?.getChannelOrNull() ?: run {
            onFailure("**:question: VC に参加、または指定してください。**")
            null
        }

    /**
     * VCSpeaker が現在参加している [VoiceChannel] を取得します。
     */
    suspend fun GuildBehavior.selfVoiceChannel() = selfMember().getVoiceStateOrNull()?.getChannelOrNull()

    /**
     * ChatCommand に [content] を返信します。
     *
     * @param content 返信する文章
     */
    suspend fun ChatCommandContext<out Arguments>.respond(content: String) = message.respond(content)

    /**
     * [BaseVoiceChannelBehavior] のチャンネル名を取得します。
     */
    suspend fun BaseVoiceChannelBehavior.getName() = this.asChannel().name

    /**
     * [Channel] がスレッドかどうか。
     */
    fun Channel.isThread() = listOf(
        ChannelType.PrivateThread,
        ChannelType.PublicGuildThread,
        ChannelType.PublicNewsThread
    ).contains(type)

    /**
     * VC の GoLive 率を計算します。
     */
    suspend fun BaseVoiceChannelBehavior.calculateGoLiveRate(): Int {
        val states = voiceStates.filter { !it.getMember().isBot && it.getMember().hasPermission(Permission.Stream) }
        val goLiveMemberCount = states.count { it.isSelfStreaming }
        val memberCount = states.count()

        return if (memberCount == 0) {
            0
        } else {
            (goLiveMemberCount.toDouble() / memberCount * 100).toInt()
        }
    }

    fun MessageBehavior.addReactionSafe(emoji: String) {
        runBlocking {
            launch {
                try {
                    addReaction(emoji)
                } catch (e: KtorRequestException) {
                    if (e.httpResponse.status.value == 403)
                        logger.warn { "Reaction Denied: ${e.message}. Message ID: $id. Ignoring..." }
                    else throw e
                }
            }
        }
    }

    fun MessageBehavior.deleteOwnReactionSafe(emoji: String) {
        runBlocking {
            launch {
                deleteOwnReaction(emoji)
            }
        }
    }

    fun Snowflake.toLong() = this.value.toLong()

    fun Long.toSnowflake() = Snowflake(this)
}