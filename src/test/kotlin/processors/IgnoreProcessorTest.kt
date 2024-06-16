package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.IgnoreData
import com.jaoafa.vcspeaker.stores.IgnoreStore
import com.jaoafa.vcspeaker.stores.IgnoreType
import com.jaoafa.vcspeaker.stores.StoreStruct
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.api.Speaker
import com.jaoafa.vcspeaker.tts.processors.IgnoreAfterReplaceProcessor
import com.jaoafa.vcspeaker.tts.processors.IgnoreBeforeReplaceProcessor
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class IgnoreProcessorTest : FunSpec({
    beforeTest {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File("./store-test")

        val storeStruct = mockk<StoreStruct<IgnoreData>>()
        every { storeStruct.write() } returns Unit

        mockkObject(IgnoreStore)
        every { IgnoreStore.write() } returns Unit

        IgnoreStore.data.clear()

        IgnoreStore.create(IgnoreData(Snowflake(0), Snowflake(123), IgnoreType.Equals, "equals"))
        IgnoreStore.create(IgnoreData(Snowflake(0), Snowflake(123), IgnoreType.Contains, "contains"))
    }

    test("When processing the IgnoreBeforeReplaceProcessor, it is canceled if an exact match is made.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreBeforeReplaceProcessor()
        processor.process(message, "equals", voice)

        processor.isCancelled() shouldBe true
    }

    test("When processing the IgnoreBeforeReplaceProcessor, it should be canceled if a partial match is made.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreBeforeReplaceProcessor()
        processor.process(message, "the text contains the word contains", voice)

        processor.isCancelled() shouldBe true
    }

    test("When processing the IgnoreBeforeReplaceProcessor, it should not be canceled if there is no match.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreBeforeReplaceProcessor()
        processor.process(message, "no match", voice)

        processor.isCancelled() shouldBe false
    }

    test("When processing the IgnoreAfterReplaceProcessor, it is canceled if an exact match is made.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreAfterReplaceProcessor()
        processor.process(message, "equals", voice)

        processor.isCancelled() shouldBe true
    }

    test("When processing the IgnoreAfterReplaceProcessor, it should be canceled if a partial match is made.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreAfterReplaceProcessor()
        processor.process(message, "the text contains the word contains", voice)

        processor.isCancelled() shouldBe true
    }

    test("When processing the IgnoreAfterReplaceProcessor, it should not be canceled if there is no match.") {
        val message = mockk<Message>()
        coEvery { message.getGuild() } returns mockk {
            every { id } returns Snowflake(0)
        }

        val voice = Voice(speaker = Speaker.Hikari)

        val processor = IgnoreAfterReplaceProcessor()
        processor.process(message, "no match", voice)

        processor.isCancelled() shouldBe false
    }
})