package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.features.EMOTION_LEVEL_DEFAULT
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.EmotionLevelOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.EmotionOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.PitchOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.SpeakerOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.SpeedOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.VolumeOption
import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.VoiceOptions
import com.jaoafa.vcspeaker.tts.EmotionData
import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.converters.impl.optionalInt
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging

class VoiceCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    class VoiceSetOptions : Options(), VoiceOptions {
        override val speaker by optionalStringChoice(SpeakerOption)
        override val emotion by optionalStringChoice(EmotionOption)
        override val emotionLevel by optionalInt(EmotionLevelOption)
        override val pitch by optionalInt(PitchOption)
        override val speed by optionalInt(SpeedOption)
        override val volume by optionalInt(VolumeOption)
    }

    override suspend fun setup() {
        publicSlashCommand("voice", "自分の声を設定します。") {
            check { anyGuild() }
            publicSubCommand("set", "自分の声を設定します。", ::VoiceSetOptions) {
                action {
                    val oldVoice = VoiceStore.byIdOrDefault(event.interaction.user.id)

                    val givenEmotion = arguments.emotion
                    val emotion = if (givenEmotion != null) {
                        if (givenEmotion == "none") null else Emotion.valueOf(givenEmotion)
                    } else oldVoice.emotion

                    val newVoice = oldVoice.copyNotNull(
                        speaker = arguments.speaker?.let { Speaker.valueOf(it) },
                        pitch = arguments.pitch,
                        speed = arguments.speed,
                        volume = arguments.volume
                    ).copy(
                        emotionData = emotion?.let {
                            EmotionData(
                                it,
                                arguments.emotionLevel ?: oldVoice.emotionLevel ?: EMOTION_LEVEL_DEFAULT
                            )
                        }
                    )

                    VoiceStore[event.interaction.user.id] = newVoice

                    val emotionEmoji = newVoice.emotion?.emoji ?: ":neutral_face:"

                    val viewOnly = oldVoice == newVoice

                    respondEmbed(
                        if (viewOnly) ":loudspeaker: Current Voice"
                        else ":arrows_counterclockwise: Voice Updated"
                    ) {
                        authorOf(user)

                        field {
                            name = ":grinning: 話者"
                            value = newVoice.speaker.speakerName
                            inline = true
                        }
                        field {
                            name = "$emotionEmoji 感情"
                            value = newVoice.emotion?.emotionName ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":signal_strength: 感情レベル"
                            value = newVoice.emotionLevel?.let { "`Level $it`" } ?: "未設定"
                            inline = true
                        }
                        field {
                            name = ":arrow_up_down: ピッチ"
                            value = newVoice.pitch.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":fast_forward: 速度"
                            value = newVoice.speed.let { "`$it%`" }
                            inline = true
                        }
                        field {
                            name = ":loud_sound: 音量"
                            value = newVoice.volume.let { "`$it%`" }
                            inline = true
                        }

                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Voice Set: @${user.username} set their voice"
                    }
                }
            }

            publicSubCommand("reset", "自分の声を初期化します。") {
                action {
                    VoiceStore.remove(user.id)

                    respondEmbed(":broom: Voice Reset", "声を初期化しました。") {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Voice Reset: @${user.username} reset their voice"
                    }
                }
            }
        }
    }
}