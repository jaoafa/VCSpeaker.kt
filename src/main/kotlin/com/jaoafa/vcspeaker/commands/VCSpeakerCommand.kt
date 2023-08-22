package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.store.GuildStore
import com.jaoafa.vcspeaker.tools.*
import com.jaoafa.vcspeaker.voicetext.Emotion
import com.jaoafa.vcspeaker.voicetext.Speaker
import com.jaoafa.vcspeaker.voicetext.Voice
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ChannelType
import kotlin.system.exitProcess

class VCSpeakerCommand : Extension() {

    override val name = this::class.simpleName!!

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
            description = "デフォルトの話者"

            for (value in Speaker.entries)
                choice(value.speakerName, value.name)
        }

        val emotion by optionalStringChoice {
            name = "emotion"
            description = "デフォルトの感情"

            for (value in Emotion.entries)
                choice(value.emotionName, value.name)
        }

        val emotionLevel by optionalInt {
            name = "emotion-level"
            description = "デフォルトの感情レベル"

            maxValue = 4
            minValue = 1
        }

        val pitch by optionalInt {
            name = "pitch"
            description = "デフォルトのピッチ"

            maxValue = 200
            minValue = 50
        }

        val speed by optionalInt {
            name = "speed"
            description = "デフォルトの速度"

            maxValue = 200
            minValue = 50
        }

        val volume by optionalInt {
            name = "volume"
            description = "デフォルトの音量"

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
            publicSubCommand("restart", "VCSpeaker を再起動します。") {

                action {
                    respond { content = ":firecracker: **再起動します。**" }
                    event.kord.shutdown()
                    exitProcess(0)
                }
            }

            publicSubCommand("settings", "VCSpeaker の設定を行います。", ::SettingsOptions) {

                action {
                    val guildId = guild!!.id
                    val current = GuildStore[guildId]
                    val currentVoice = current?.voice

                    // option > current > default
                    val guildData = GuildStore.createOrUpdate(
                        guildId = guildId,
                        channelId = arguments.channel?.id ?: current?.channelId,
                        prefix = arguments.prefix ?: current?.prefix,
                        voice = arguments.run {
                            Voice(
                                speaker = Speaker.valueOf(speaker ?: currentVoice?.speaker?.name ?: "Haruka"),
                                emotion = if (emotion != null) Emotion.valueOf(emotion!!) else currentVoice?.emotion,
                                emotionLevel = emotionLevel ?: currentVoice?.emotionLevel ?: 2,
                                pitch = pitch ?: currentVoice?.pitch ?: 100,
                                speed = speed ?: currentVoice?.speed ?: 100,
                                volume = volume ?: currentVoice?.volume ?: 100
                            )
                        },
                        autoJoin = arguments.autoJoin ?: current?.autoJoin ?: true
                    )

                    val emotionEmoji = when (guildData.voice.emotion) {
                        Emotion.Happiness -> ":grinning:"
                        Emotion.Anger -> ":face_with_symbols_over_mouth:"
                        Emotion.Sadness -> ":pensive:"
                        null -> ":neutral_face:"
                    }

                    respondEmbed(":repeat: 設定を更新しました") {
                        authorOf(user)

                        field {
                            name = ":hash: 読み上げチャンネル"
                            value = guildData.channelId?.let { guild!!.getChannelOrNull(it)?.mention } ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":symbols: プレフィックス"
                            value = guildData.prefix?.let { "`$it`" } ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":grinning: 話者"
                            value = guildData.voice.speaker.speakerName
                            inline = true
                        }
                        field {
                            name = "$emotionEmoji 感情"
                            value = guildData.voice.emotion?.emotionName ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":signal_strength: 感情レベル"
                            value = guildData.voice.emotionLevel.let { "`Level $it`" }
                            inline = true
                        }
                        field {
                            name = ":arrow_up_down: ピッチ"
                            value = guildData.voice.pitch.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":fast_forward: 速度"
                            value = guildData.voice.speed.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":loud_sound: 音量"
                            value = guildData.voice.volume.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":inbox_tray: 自動入退室"
                            value = if (guildData.autoJoin) "有効" else "無効"
                            inline = true
                        }

                        successColor()
                    }
                }
            }
        }
    }
}