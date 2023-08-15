package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.store.VoiceStore
import com.jaoafa.vcspeaker.tools.*
import com.jaoafa.vcspeaker.voicetext.Emotion
import com.jaoafa.vcspeaker.voicetext.Speaker
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.extensions.Extension

class VoiceCommand : Extension() {

    override val name = this::class.simpleName!!

    inner class VoiceOptions : Options() {
        val speaker by optionalStringChoice {
            name = "speaker"
            description = "デフォルトの話者"

            for (value in Speaker.values())
                choice(value.speakerName, value.name)
        }

        val emotion by optionalStringChoice {
            name = "emotion"
            description = "デフォルトの感情"

            for (value in Emotion.values())
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
    }

    override suspend fun setup() {
        publicSlashCommand("voice", "自分の声を設定します。", ::VoiceOptions) {

            devGuild()

            action {
                val voice = VoiceStore.byIdOrDefault(event.interaction.user.id).copy(
                    speaker = arguments.speaker?.let { Speaker.valueOf(it) } ?: Speaker.Hikari,
                    emotion = arguments.emotion?.let { Emotion.valueOf(it) },
                    emotionLevel = arguments.emotionLevel ?: 2,
                    pitch = arguments.pitch ?: 100,
                    speed = arguments.speed ?: 100,
                    volume = arguments.volume ?: 100
                )

                VoiceStore[event.interaction.user.id] = voice

                val emotionEmoji = when (voice.emotion) {
                    Emotion.Happiness -> ":grinning:"
                    Emotion.Anger -> ":face_with_symbols_over_mouth:"
                    Emotion.Sadness -> ":pensive:"
                    null -> ":neutral_face:"
                }

                respondEmbed(":repeat: あなたの声を更新しました") {
                    authorOf(user)

                    field {
                        name = ":grinning: 話者"
                        value = voice.speaker.speakerName
                        inline = true
                    }
                    field {
                        name = "$emotionEmoji 感情"
                        value = voice.emotion?.emotionName ?: "未設定"
                        inline = true
                    }
                    field {
                        name = ":signal_strength: 感情レベル"
                        value = voice.emotionLevel.let { "`Level $it`" }
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

                    successColor()
                }
            }
        }
    }
}