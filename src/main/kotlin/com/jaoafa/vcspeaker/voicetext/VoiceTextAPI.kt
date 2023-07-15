package com.jaoafa.vcspeaker.voicetext

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.config.TokenSpec
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

class VoiceTextAPI(private val apiKey: String) {
    private val baseURL = "https://api.voicetext.jp/v1/tts"
    private val client = HttpClient(CIO)

    suspend fun generateSpeech(
        text: String,
        parameter: VoiceParameter
    ): ByteArray {
        val response = client.post(baseURL) {
            parameter("text", text)
            Json.parseToJsonElement(parameter.toJson()).jsonObject.toMap().forEach { (t, u) ->
                parameter(t, u.jsonPrimitive.content)
            }
            basicAuth(apiKey, "")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<ByteArray>()

            else -> throw Exception(
                Json.decodeFromString(
                    VoiceTextError.serializer(),
                    response.bodyAsText()
                ).error.message
            )
        }
    }
}

class VoiceTextAPITest {
    @Test
    fun test() {
        val config = Config { addSpec(TokenSpec) }.from.yaml.file("./config.yml")

        VCSpeaker.voiceText = VoiceTextAPI(config[TokenSpec.voicetext])

        val voice = runBlocking {
            VCSpeaker.voiceText.generateSpeech(
                text = "どこどこだ！！┗(^o^;)┓どこどこかな？？？？ｗｗｗ┏(;^o^)┛どこどこかな？？？？ｗｗｗ(´･｀;)こ…これ…これは…………どこどこだあああああ┗(^o^)┛ｗｗｗｗｗ┏(^o^)┓どこどこどこどこｗｗｗｗ",
                parameter = VoiceParameter(
                    speaker = Speaker.HIKARI,
                    emotion = Emotion.ANGER,
                    emotionLevel = 3,
                    pitch = 100,
                    speed = 100,
                    volume = 100
                )
            )
        }

        File("./test.wav").writeBytes(voice)
    }
}