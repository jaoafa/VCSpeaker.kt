package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.tts.providers.voicetext.Emotion
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kordex.core.commands.application.slash.converters.impl.OptionalStringChoiceConverterBuilder
import dev.kordex.core.commands.converters.impl.OptionalIntConverterBuilder

const val EMOTION_LEVEL_MAX = 4
const val EMOTION_LEVEL_MIN = 1
const val PITCH_MAX = 200
const val PITCH_MIN = 50
const val SPEED_MAX = 200
const val SPEED_MIN = 50
const val VOLUME_MAX = 200
const val VOLUME_MIN = 50

const val EMOTION_LEVEL_DEFAULT = 2
const val PITCH_DEFAULT = 100
const val SPEED_DEFAULT = 120
const val VOLUME_DEFAULT = 100

object Voice {
    object CommandOptions {
        val SpeakerOption: OptionalStringChoiceConverterBuilder.() -> Unit = {
            name = "speaker"
            description = "話者"

            for (value in Speaker.entries)
                choice(value.speakerName, value.name)
        }

        val EmotionOption: OptionalStringChoiceConverterBuilder.() -> Unit = {
            name = "emotion"
            description = "感情"

            for (value in Emotion.entries)
                choice(value.emotionName, value.name)

            choice("なし", "none")
        }

        val EmotionLevelOption: OptionalIntConverterBuilder.() -> Unit = {
            name = "emotion-level"
            description = "感情レベル"

            maxValue = EMOTION_LEVEL_MAX
            minValue = EMOTION_LEVEL_MIN
        }

        val PitchOption: OptionalIntConverterBuilder.() -> Unit = {
            name = "pitch"
            description = "ピッチ"

            maxValue = PITCH_MAX
            minValue = PITCH_MIN
        }

        val SpeedOption: OptionalIntConverterBuilder.() -> Unit = {
            name = "speed"
            description = "速度"

            maxValue = SPEED_MAX
            minValue = SPEED_MIN
        }

        val VolumeOption: OptionalIntConverterBuilder.() -> Unit = {
            name = "volume"
            description = "音量"

            maxValue = VOLUME_MAX
            minValue = VOLUME_MIN
        }
    }
}