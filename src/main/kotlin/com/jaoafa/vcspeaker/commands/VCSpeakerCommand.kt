package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.asChannelOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.errorColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.infoColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respond
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.warningColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.channel.TextChannel
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
import kotlin.system.exitProcess

class VCSpeakerCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    inner class SettingsOptions : Options() {
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

            publicSubCommand("settings", "VCSpeaker を設定します。", ::SettingsOptions) {
                action {
                    val guildId = guild!!.id
                    val oldGuildData = GuildStore[guildId]
                    val currentVoice = oldGuildData?.voice

                    // option > current > default
                    val newGuildData = GuildStore.createOrUpdate(
                        guildId = guildId,
                        channelId = arguments.channel?.id ?: oldGuildData?.channelId,
                        prefix = arguments.prefix ?: oldGuildData?.prefix,
                        voice = arguments.run {
                            val newEmotion = if (emotion == "none") {
                                null
                            } else if (emotion != null) {
                                Emotion.valueOf(emotion!!)
                            } else {
                                currentVoice?.emotion
                            }

                            val newEmotionLevel = if (newEmotion != null) {
                                emotionLevel ?: currentVoice?.emotionLevel
                            } else null

                            Voice(
                                speaker = Speaker.valueOf(speaker ?: currentVoice?.speaker?.name ?: "Haruka"),
                                emotion = newEmotion,
                                emotionLevel = newEmotionLevel ?: 2,
                                pitch = pitch ?: currentVoice?.pitch ?: 100,
                                speed = speed ?: currentVoice?.speed ?: 100,
                                volume = volume ?: currentVoice?.volume ?: 100
                            )
                        },
                        autoJoin = arguments.autoJoin ?: oldGuildData?.autoJoin ?: true
                    )

                    val emotionEmoji = newGuildData.voice.emotion?.emoji ?: ":neutral_face:"

                    val viewOnly = oldGuildData == newGuildData

                    respondEmbed(
                        if (viewOnly) ":gear: Current Settings"
                        else ":arrows_counterclockwise: Settings Updated"
                    ) {
                        authorOf(user)

                        // fixme redundant
                        // todo settings diff
                        field {
                            name = ":hash: 読み上げチャンネル"
                            value = newGuildData.channelId?.asChannelOf<TextChannel>()?.mention ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":symbols: プレフィックス"
                            value = newGuildData.prefix?.let { "`$it`" } ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":grinning: 話者"
                            value = newGuildData.voice.speaker.speakerName
                            inline = true
                        }
                        field {
                            name = "$emotionEmoji 感情"
                            value = newGuildData.voice.emotion?.emotionName ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":signal_strength: 感情レベル"
                            value = newGuildData.voice.emotionLevel.let { "`Level $it`" }
                            inline = true
                        }
                        field {
                            name = ":arrow_up_down: ピッチ"
                            value = newGuildData.voice.pitch.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":fast_forward: 速度"
                            value = newGuildData.voice.speed.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":loud_sound: 音量"
                            value = newGuildData.voice.volume.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":inbox_tray: 自動入退室"
                            value = if (newGuildData.autoJoin) "有効" else "無効"
                            inline = true
                        }

                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Settings Updated: Settings updated by @${user.username}"
                    }
                }
            }

            publicSubCommand("remove", "VCSpeaker の登録を削除します。") {
                action {
                    val guildId = guild!!.id
                    val guildData = GuildStore[guildId]

                    if (guildData == null) {
                        respond("**:x: このサーバーは登録されていません。**")
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

                                    // 各種データを削除
                                    AliasStore.removeForGuild(guildId)
                                    IgnoreStore.removeForGuild(guildId)
                                    ReadableBotStore.removeForGuild(guildId)
                                    TitleStore.removeForGuild(guildId)
                                    GuildStore.remove(guildData)
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