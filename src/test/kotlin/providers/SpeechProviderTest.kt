package providers

import com.jaoafa.vcspeaker.tts.providers.*
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class SpeechProviderTest : FunSpec({
    test("hashMd5 should compute correct MD5 hash") {
        val input = "hello"
        val expected = "5d41402abc4b2a76b9719d911017c592"
        hashMd5(input) shouldBe expected
    }

    test("providerOf should return SoundmojiProvider for SoundmojiContext") {
        val ctx = SoundmojiContext(Snowflake(123456))
        val provider = providerOf(ctx)
        provider shouldBe SoundmojiProvider
    }

    test("providerOf should return VoiceTextProvider for VoiceTextContext") {
        val voice = Voice(speaker = Speaker.Hikari)
        val ctx = VoiceTextContext(voice, "text")
        val provider = providerOf(ctx)
        provider shouldBe VoiceTextProvider
    }

    test("providerOf should throw for unknown context") {
        val dummy = object : ProviderContext {
            override fun describe(): String = "dummy"
            override fun identity(): String = "dummy"
        }
        shouldThrow<IllegalArgumentException> {
            providerOf(dummy)
        }
    }

    test("getProvider should return correct provider or null") {
        getProvider(SoundmojiProvider.id) shouldBe SoundmojiProvider
        getProvider(VoiceTextProvider.id) shouldBe VoiceTextProvider
        getProvider("unknown") shouldBe null
    }
})
