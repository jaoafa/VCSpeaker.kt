package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.guildParameterOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.infoColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.warningColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.converters.impl.optionalBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.optionalInt
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.components.components
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.system.exitProcess

class VCSpeakerCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    class SettingsOptions : Options() {
        val channel by optionalChannel {
            name = "channel"
            description = "読み上げるテキストチャンネル"
            requireChannelType(ChannelType.GuildText)
        }

        val prefix by optionalString {
            name = "prefix"
            description = "チャットコマンドのプレフィックス"
        }

        val speaker by optionalStringChoice {
            name = "speaker"
            description = "話者"

            for (value in Speaker.entries)
                choice(value.speakerName, value.name)
        }

        val emotion by optionalStringChoice {
            name = "emotion"
            description = "感情"

            for (value in Emotion.entries)
                choice(value.emotionName, value.name)

            choice("なし", "none")
        }

        val emotionLevel by optionalInt {
            name = "emotion-level"
            description = "感情レベル"

            maxValue = 4
            minValue = 1
        }

        val pitch by optionalInt {
            name = "pitch"
            description = "ピッチ"

            maxValue = 200
            minValue = 50
        }

        val speed by optionalInt {
            name = "speed"
            description = "速度"

            maxValue = 200
            minValue = 50
        }

        val volume by optionalInt {
            name = "volume"
            description = "音量"

            maxValue = 200
            minValue = 50
        }

        val autoJoin by optionalBoolean {
            name = "auto-join"
            description = "VC に自動で入退室するかどうか"
        }
    }

    override suspend fun setup() {
        publicSlashCommand("vcspeaker", "VCSpeaker を操作します。") {
            check { anyGuild() }

            publicSubCommand("restart", "VCSpeaker を再起動します。") {
                action {
                    respond("**:firecracker: 再起動します。**")
                    event.kord.shutdown()
                    exitProcess(0)
                }
            }

            publicSubCommand("register", "このサーバに VCSpeaker を登録します。") {
                action {
                    val guildId = guild?.id ?: return@action

                    val registered = transaction {
                        GuildEntity.findById(guildId)
                    }

                    if (registered != null) {
                        respondEmbed(
                            ":x: Already Registered",
                            "このサーバーはすでに登録されています。"
                        ) {
                            authorOf(user)
                            guildParameterOf(registered)
                            errorColor()
                        }

                        return@action
                    }

                    val guildEntity = transaction {
                        val voice = VoiceEntity.new {
                            speaker = Speaker.Haruka
                            emotion = null
                            emotionLevel = null
                        }
                        GuildEntity.new(id = guildId) {
                            speakerVoiceEntity = voice
                        }
                    }

                    respondEmbed(
                        ":white_check_mark: Registration Successful",
                        "登録が完了しました！"
                    ) {
                        authorOf(user)
                        guildParameterOf(guildEntity)
                        successColor()
                    }
                }
            }

            publicSubCommand("settings", "VCSpeaker を設定します。", ::SettingsOptions) {
                action {
                    val guildId = guild?.id ?: return@action

                    val guildEntity = transaction {
                        GuildEntity.findById(guildId)
                    }

                    if (guildEntity == null) {
                        respondEmbed(
                            ":x: Not Registered",
                            "このサーバーは登録されていません。先に `/vcspeaker register` コマンドを実行してください。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    var modified = false

                    transaction {
                        // modifies GuildEntity
                        guildEntity.run {
                            // if the argument exists (non-null), update it
                            arguments.channel?.id?.also { channelDid = it; modified = true }
                            arguments.prefix?.also { prefix = it; modified = true }
                            arguments.autoJoin?.also { autoJoin = it; modified = true }
                        }

                        guildEntity.speakerVoiceEntity.run {
                            // if emotion is set to be null, also set emotion level to null and stop modification lambda
                            if (arguments.emotion == "none") {
                                emotion = null
                                emotionLevel = null
                                modified = true
                                return@run
                            }

                            arguments.emotion?.also { emotion = Emotion.valueOf(it); modified = true }

                            arguments.emotionLevel?.takeIf { emotion != null }?.also {
                                emotionLevel = it
                                modified = true
                            }

                            arguments.pitch?.also { pitch = it; modified = true }
                            arguments.speed?.also { speed = it; modified = true }
                            arguments.volume?.also { volume = it; modified = true }
                        }
                    }

                    respondEmbed(
                        if (!modified) ":gear: Current Settings"
                        else ":arrows_counterclockwise: Settings Updated"
                    ) {
                        authorOf(user)
                        guildParameterOf(guildEntity)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Settings Updated: Settings updated by @${user.username}"
                    }
                }
            }

            publicSubCommand("remove", "VCSpeaker の登録を削除します。") {
                action {
                    val guildId = guild?.id ?: return@action
                    val guildEntity = transaction {
                        GuildEntity.findById(guildId)
                    }

                    if (guildEntity == null) {
                        respondEmbed(
                            ":x: Not Registered",
                            "このサーバーは登録されていません。先に `/vcspeaker register` コマンドを実行してください。"
                        ) {
                            authorOf(user)
                            errorColor()
                        }
                        return@action
                    }

                    val confirmUser = user

                    // 削除する前に確認を設ける
                    respond {
                        embed {
                            title = ":wastebasket: Confirm registration removal"
                            description = "VCSpeaker の登録を削除しますが、よろしいですか？"
                            authorOf(user)
                            warningColor()
                        }

                        components {
                            publicButton {
                                label = "はい"
                                style = ButtonStyle.Primary
                                deferredAck = true
                                action buttonAction@{
                                    // 異なるユーザーが操作できないようにする
                                    if (user.id != confirmUser.id) {
                                        respondEmbed(
                                            ":x: Failed to remove registration",
                                            "この操作は実行者のみが実行できます。"
                                        ) {
                                            authorOf(user)

                                            errorColor()
                                        }

                                        return@buttonAction
                                    }

                                    transaction {
                                        val voiceEntity = guildEntity.speakerVoiceEntity
                                        guildEntity.delete()
                                        voiceEntity.delete()
                                    }

                                    // 各種データを削除
                                    AliasStore.removeForGuild(guildId)
                                    IgnoreStore.removeForGuild(guildId)
                                    ReadableBotStore.removeForGuild(guildId)
                                    ReadableChannelStore.removeForGuild(guildId)
                                    TitleStore.removeForGuild(guildId)
                                    // GuildStore.remove(guildData)

                                    transaction {  }
                                    edit {
                                        embed {
                                            title = ":wastebasket: Registration removed"
                                            description = "VCSpeaker の登録を削除しました。"

                                            authorOf(user)
                                            successColor()
                                        }

                                        components {}
                                    }

                                    log(logger) { guild, user ->
                                        "[${guild.name}] Registration Removed: Removed by @${user.username}"
                                    }
                                }
                            }

                            publicButton {
                                label = "いいえ"
                                style = ButtonStyle.Danger
                                action buttonAction@{
                                    if (user.id != confirmUser.id) {
                                        respondEmbed(
                                            ":x: Failed to remove registration",
                                            "この操作は実行者のみが実行できます。"
                                        ) {
                                            authorOf(user)

                                            errorColor()
                                        }

                                        return@buttonAction
                                    }

                                    edit {
                                        embed {
                                            title = ":wastebasket: Registration removal canceled"
                                            description = "VCSpeaker の登録削除をキャンセルしました。"

                                            authorOf(user)
                                            infoColor()
                                        }

                                        components {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}