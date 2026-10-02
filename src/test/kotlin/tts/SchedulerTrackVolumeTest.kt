package tts

import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext
import com.jaoafa.vcspeaker.tts.trackVolume
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.useAllTables

class SchedulerTrackVolumeTest : FunSpec({
    useAllTables()

    val guildId = Snowflake(1)

    fun registerGuild(volume: Int) = transaction {
        GuildEntity.new(guildId) {
            speakerVoiceEntity = VoiceEntity.new {}
            soundboardVolume = volume
        }
    }

    test("SoundmojiContext uses the guild's soundboardVolume") {
        registerGuild(50)

        trackVolume(guildId, SoundmojiContext(Snowflake(123))) shouldBe 50
    }

    test("non-Soundmoji context always plays at volume 100") {
        registerGuild(30)

        val context = VoiceTextContext(Voice(speaker = Speaker.Hikari), "hello")

        trackVolume(guildId, context) shouldBe 100
    }

    test("soundboardVolume of 0 is clamped to 1 to avoid the lavakord 1..1000 constraint") {
        registerGuild(0)

        trackVolume(guildId, SoundmojiContext(Snowflake(123))) shouldBe 1
    }

    test("unregistered guild falls back to the default volume") {
        trackVolume(guildId, SoundmojiContext(Snowflake(123))) shouldBe 50
    }
})
