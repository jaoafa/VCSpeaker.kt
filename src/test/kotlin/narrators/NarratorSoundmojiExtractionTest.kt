package narrators

import com.jaoafa.vcspeaker.tts.Scheduler
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.narrators.Narrator
import com.jaoafa.vcspeaker.tts.providers.ProviderContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import dev.kord.common.entity.Snowflake
import dev.schlaubi.lavakord.audio.Link
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class NarratorSoundmojiExtractionTest : FunSpec({
    test("appendContextsFromText should add only text when no sound tags exist") {
        val link = mockk<Link>(relaxed = true)
        val scheduler = mockk<Scheduler>(relaxed = true)
        val narrator = Narrator(Snowflake(1), Snowflake(2), link, scheduler)

        val contexts = mutableListOf<ProviderContext>()
        val voice = Voice(speaker = Speaker.Hikari)

        val method = Narrator::class.java.getDeclaredMethod(
            "appendContextsFromText",
            String::class.java,
            Voice::class.java,
            MutableList::class.java
        )
        method.isAccessible = true
        method.invoke(narrator, "hello", voice, contexts)

        contexts shouldBe listOf(VoiceTextContext(voice, "hello"))
    }

    test("appendContextsFromText should handle a single sound tag only") {
        val link = mockk<Link>(relaxed = true)
        val scheduler = mockk<Scheduler>(relaxed = true)
        val narrator = Narrator(Snowflake(1), Snowflake(2), link, scheduler)

        val contexts = mutableListOf<ProviderContext>()
        val voice = Voice(speaker = Speaker.Hikari)

        val method = Narrator::class.java.getDeclaredMethod(
            "appendContextsFromText",
            String::class.java,
            Voice::class.java,
            MutableList::class.java
        )
        method.isAccessible = true
        method.invoke(narrator, "<sound:0:123456789012345678>", voice, contexts)

        contexts shouldBe listOf(SoundmojiContext(Snowflake(123456789012345678)))
    }

    test("appendContextsFromText should split sound tags into contexts in order") {
        val link = mockk<Link>(relaxed = true)
        val scheduler = mockk<Scheduler>(relaxed = true)
        val narrator = Narrator(Snowflake(1), Snowflake(2), link, scheduler)

        val contexts = mutableListOf<ProviderContext>()
        val voice = Voice(speaker = Speaker.Hikari)

        val method = Narrator::class.java.getDeclaredMethod(
            "appendContextsFromText",
            String::class.java,
            Voice::class.java,
            MutableList::class.java
        )
        method.isAccessible = true
        method.invoke(
            narrator,
            "AAA<sound:0:123456789012345678>BBB<sound:0:987654321098765432>",
            voice,
            contexts
        )

        contexts shouldBe listOf(
            VoiceTextContext(voice, "AAA"),
            SoundmojiContext(Snowflake(123456789012345678)),
            VoiceTextContext(voice, "BBB"),
            SoundmojiContext(Snowflake(987654321098765432))
        )
    }
})
