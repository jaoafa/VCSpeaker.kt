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
import io.mockk.*
import java.io.File

/**
 * IgnoreProcessorのテスト
 */
class IgnoreProcessorTest : FunSpec({
    // テスト前処理
    beforeTest {
        mockkObject(VCSpeaker)

        // storeFolderを一時ディレクトリに設定
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")

        val storeStruct = mockk<StoreStruct<IgnoreData>>()
        every { storeStruct.write() } returns Unit

        mockkObject(IgnoreStore)
        every { IgnoreStore.write() } returns Unit

        IgnoreStore.data.clear()

        // テスト用データを作成
        // equals という文字列で一致する場合無視
        IgnoreStore.create(IgnoreData(Snowflake(0), Snowflake(123), IgnoreType.Equals, "equals"))
        // contains という文字列を含む場合無視
        IgnoreStore.create(IgnoreData(Snowflake(0), Snowflake(123), IgnoreType.Contains, "contains"))
    }

    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // 全てのテスト後にフォルダを削除
    finalizeSpec {
        VCSpeaker.storeFolder.deleteRecursively()
    }

    context("IgnoreBeforeReplaceProcessor") {
        // 完全に一致する場合はキャンセルされる
        test("If the text exactly matches to Ignore entry, the process should be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "equals", voice)

            processor.isCancelled() shouldBe true
        }

        // 部分一致がある場合はキャンセルされる
        test("If the text partially matches to Ignore entry, the process should be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "the text contains the word contains", voice)

            processor.isCancelled() shouldBe true
        }

        // 一致するものがない場合はキャンセルされない
        test("If did not find any Ignore entries matches to the text, the process should not be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreBeforeReplaceProcessor()
            processor.process(message, "no match", voice)

            processor.isCancelled() shouldBe false
        }
    }

    context("IgnoreAfterReplaceProcessor") {
        // 完全に一致する場合はキャンセルされる
        test("If the text exactly matches to Ignore entry, the process should be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "equals", voice)

            processor.isCancelled() shouldBe true
        }

        // 部分一致がある場合はキャンセルされる
        test("If the text partially matches to Ignore entry, the process should be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "the text contains the word contains", voice)

            processor.isCancelled() shouldBe true
        }

        // 一致するものがない場合はキャンセルされない
        test("If did not find any Ignore entries matches to the text, the process should not be cancelled.") {
            val message = mockk<Message>()
            coEvery { message.getGuild() } returns mockk {
                every { id } returns Snowflake(0)
            }

            val voice = Voice(speaker = Speaker.Hikari)

            val processor = IgnoreAfterReplaceProcessor()
            processor.process(message, "no match", voice)

            processor.isCancelled() shouldBe false
        }
    }
})