package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.stores.VoiceStore
import com.jaoafa.vcspeaker.tools.Discord.authorOf
import com.jaoafa.vcspeaker.tools.Discord.publicSlashCommand
import com.jaoafa.vcspeaker.tools.Discord.respondEmbed
import com.jaoafa.vcspeaker.tools.Discord.successColor
import com.jaoafa.vcspeaker.tools.Options
import com.jaoafa.vcspeaker.voicetext.api.Emotion
import com.jaoafa.vcspeaker.voicetext.api.Speaker
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.extensions.Extension

class VoiceCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class VoiceOptions : Options() {
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
    }

    override suspend fun setup() {
        publicSlashCommand("voice", "自分の声を設定します。", ::VoiceOptions) {
            action {
                val oldVoice = VoiceStore.byIdOrDefault(event.interaction.user.id)

                val newVoice = VoiceStore.byIdOrDefault(event.interaction.user.id).copy(
                    speaker = arguments.speaker?.let { Speaker.valueOf(it) } ?: oldVoice.speaker,
                    emotion = arguments.emotion?.let { Emotion.valueOf(it) } ?: oldVoice.emotion,
                    emotionLevel = arguments.emotionLevel ?: oldVoice.emotionLevel,
                    pitch = arguments.pitch ?: oldVoice.pitch,
                    speed = arguments.speed ?: oldVoice.speed,
                    volume = arguments.volume ?: oldVoice.volume
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
                        value = newVoice.emotionLevel.let { "`Level $it`" }
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
            }
        }
    }
}