package tts

import com.jaoafa.vcspeaker.stores.GuildData
import com.jaoafa.vcspeaker.stores.GuildStore
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.jaoafa.vcspeaker.tts.trackVolume
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject

class SchedulerTrackVolumeTest : FunSpec({
    val guildId = Snowflake(1)

    fun guildData(soundboardVolume: Int) = GuildData(
        guildId = guildId,
        channelId = null,
        prefix = null,
        voice = Voice(speaker = Speaker.Hikari),
        autoJoin = false,
        soundboardVolume = soundboardVolume
    )

    afterTest { unmockkObject(GuildStore) }

    test("SoundmojiContext uses the guild's soundboardVolume") {
        mockkObject(GuildStore)
        coEvery { GuildStore.getOrDefault(guildId) } returns guildData(50)

        trackVolume(guildId, SoundmojiContext(Snowflake(123))) shouldBe 50
    }

    test("non-Soundmoji context always plays at volume 100") {
        mockkObject(GuildStore)
        coEvery { GuildStore.getOrDefault(guildId) } returns guildData(30)

        val context = VoiceTextContext(Voice(speaker = Speaker.Hikari), "hello")

        trackVolume(guildId, context) shouldBe 100
    }

    test("soundboardVolume of 0 is clamped to 1 to avoid the lavakord 1..1000 constraint") {
        mockkObject(GuildStore)
        coEvery { GuildStore.getOrDefault(guildId) } returns guildData(0)

        trackVolume(guildId, SoundmojiContext(Snowflake(123))) shouldBe 1
    }
})
