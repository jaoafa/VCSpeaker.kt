package com.jaoafa.vcspeaker.tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GuildStore
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.PublicSlashCommandContext
import dev.kordex.core.commands.chat.ChatCommandContext
import dev.kordex.core.types.PublicInteractionContext
import dev.kordex.core.utils.hasPermission
import dev.kordex.core.utils.respond
import dev.kordex.core.utils.selfMember
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter

typealias Options = Arguments

/**
 * Discord 関連の拡張関数をまとめたオブジェクト。
 */
object DiscordExtensions {
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
        builder: suspend EmbedBuilder.() -> Unit = {}
    ) = this.respond {
        embed(title, description, builder)
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
        builder: suspend EmbedBuilder.() -> Unit = {}
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
    suspend fun BaseVoiceChannelBehavior.name() = this.asChannel().name

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
}