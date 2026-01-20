package soundmoji

import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiUtils
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SoundmojiUtilsTest : FunSpec({
    test("normalizeSoundmojiReferences should convert a raw id to a sound tag") {
        val input = "123456789012345678"
        SoundmojiUtils.normalizeSoundmojiReferences(input) shouldBe "<sound:0:123456789012345678>"
    }

    test("normalizeSoundmojiReferences should convert a CDN URL to a sound tag") {
        val input = "AAA https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3?x=1 BBB"
        SoundmojiUtils.normalizeSoundmojiReferences(input) shouldBe "AAA <sound:0:123456789012345678> BBB"
    }

    test("normalizeSoundmojiReferences should convert multiple CDN URLs") {
        val input = "A https://cdn.discordapp.com/soundboard-sounds/111.mp3 B https://cdn.discordapp.com/soundboard-sounds/222"
        SoundmojiUtils.normalizeSoundmojiReferences(input) shouldBe "A <sound:0:111> B <sound:0:222>"
    }

    test("normalizeSoundmojiReferences should keep existing sound tags") {
        val input = "X <sound:0:123456789012345678> Y"
        SoundmojiUtils.normalizeSoundmojiReferences(input) shouldBe input
    }

    test("containsSoundmojiReference should detect sound tags") {
        SoundmojiUtils.containsSoundmojiReference("AAA<sound:0:123456789012345678>BBB") shouldBe true
        SoundmojiUtils.containsSoundmojiReference("AAA") shouldBe false
    }

    test("containsSoundmojiReference should detect CDN URLs and raw ids") {
        SoundmojiUtils.containsSoundmojiReference("https://cdn.discordapp.com/soundboard-sounds/123456789012345678") shouldBe true
        SoundmojiUtils.containsSoundmojiReference("123456789012345678") shouldBe true
    }

    test("containsSoundmojiReference should not match numbers mixed with text") {
        SoundmojiUtils.containsSoundmojiReference("id:123456789012345678") shouldBe false
    }
})
