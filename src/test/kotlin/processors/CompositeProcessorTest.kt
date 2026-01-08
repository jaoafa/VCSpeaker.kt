package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.*
import com.jaoafa.vcspeaker.tools.getClassesIn
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.processors.BaseProcessor
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.replacers.UrlReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.io.File
import kotlin.reflect.full.createInstance

/**
 * Processorの複合テスト
 *
 * 複数のProcessorが組み合わさった場合の挙動をテストします。
 * これは、ユーザーが実際に操作したときの最終的な挙動に近いテストです。
 */
class CompositeProcessorTest : FunSpec({
    // テスト前処理
    beforeTest {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")

        val storeStruct = mockk<StoreStruct<IgnoreData>>()
        every { storeStruct.write() } returns Unit

        mockkObject(IgnoreStore)
        every { IgnoreStore.write() } returns Unit
        IgnoreStore.data.clear()

        mockkObject(AliasStore)
        every { AliasStore.write() } returns Unit
        AliasStore.data.clear()
    }

    // テスト後にモックを削除
    afterTest {
        clearAllMocks()
    }

    // 全てのテスト後にフォルダを削除
    finalizeSpec {
        VCSpeaker.storeFolder.deleteRecursively()
    }

    /**
     * プロセッサチェーン全体を通してメッセージを処理するヘルパー関数
     */
    suspend fun processWithAllProcessors(message: Message, text: String, voice: Voice): Pair<String, Voice>? {
        val processors = getClassesIn<BaseProcessor>("com.jaoafa.vcspeaker.tts.processors")
            .mapNotNull {
                it.kotlin.createInstance()
            }.sortedBy { it.priority }

        return processors.fold(text to voice) { (processText, processVoice), processor ->
            val (processedText, processedVoice) = processor.process(message, processText, processVoice)
            if (processor.isCancelled()) return null
            if (processor.isImmediately()) return processedText to processedVoice

            processedText to processedVoice
        }
    }

    /**
     * テスト用のシンプルなメッセージモックを作成するヘルパー関数
     */
    fun createSimpleMessageMock(guildId: Snowflake): Message {
        val message = mockk<Message>(relaxed = true)
        coEvery { message.getGuild() } returns mockk {
            every { id } returns guildId
        }
        every { message.type } returns dev.kord.common.entity.MessageType.Default
        every { message.stickers } returns emptyList()
        every { message.attachments } returns emptySet()
        every { message.flags } returns null
        every { message.messageReference } returns null
        every { message.referencedMessage } returns null
        return message
    }

    context("Ignore と Alias の複合テスト") {
        // 1. ignoreされたテキストが、エイリアス対象であったとしても無視されること
        test("If the text is ignored, it should remain ignored even if it matches an alias pattern") {
            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "hello" という文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Equals,
                    search = "hello"
                )
            )

            // "hello" を "こんにちは" に置き換えるエイリアス設定
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "hello",
                    replace = "こんにちは"
                )
            )

            // IgnoreBeforeReplaceProcessor (priority 70) で無視されるため、
            // ReplacerProcessor (priority 80) でエイリアスは適用されず、
            // プロセッサチェーンはnullを返す
            val result = processWithAllProcessors(message, "hello", voice)
            result.shouldBeNull()
        }

        // 2. 部分一致でignoreされたテキストが、エイリアス対象であったとしても無視されること
        test("If the text partially matches ignore pattern, it should be ignored even if it contains alias") {
            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "ignore" を含む文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Contains,
                    search = "ignore"
                )
            )

            // "test" を "テスト" に置き換えるエイリアス設定
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "test",
                    replace = "テスト"
                )
            )

            // "test" が含まれているが "ignore" も含まれているため、
            // IgnoreBeforeReplaceProcessorで無視される
            val result = processWithAllProcessors(message, "this is ignore test", voice)
            result.shouldBeNull()
        }
    }

    context("URL置き換えとIgnoreの複合テスト") {
        // 3. URL置き換えなどでignore対象になった場合、無視されること
        test("If URL replacement results in text matching ignore pattern, it should be ignored") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "Webページ" を含む文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Contains,
                    search = "Webページ"
                )
            )

            // URLは IgnoreBeforeReplaceProcessor (priority 70) の時点では無視されないが、
            // ReplacerProcessor (priority 80) で "Webページ「Example Domain」へのリンク" に変換され、
            // IgnoreAfterReplaceProcessor (priority 90) で無視される
            val result = processWithAllProcessors(message, "Check this: https://example.com", voice)
            result.shouldBeNull()
        }

        // 4. URL置き換え後にエイリアスが適用されてからIgnoreされること
        test("URL replacement should apply, then alias, then ignore check") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "Example Domain" を "例のドメイン" に置き換えるエイリアス
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "Example Domain",
                    replace = "例のドメイン"
                )
            )

            // "例のドメイン" を含む文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Contains,
                    search = "例のドメイン"
                )
            )

            // ReplacerProcessor で URL → "Webページ「Example Domain」へのリンク" → (エイリアス) "Webページ「例のドメイン」へのリンク"
            // IgnoreAfterReplaceProcessor で "例のドメイン" を含むので無視される
            val result = processWithAllProcessors(message, "Visit https://example.com", voice)
            result.shouldBeNull()
        }
    }

    context("Aliasが適用されてからIgnoreチェックされること") {
        // 5. エイリアス置き換え後の文字列がignore対象になる場合、無視されること
        test("If alias replacement results in text matching ignore pattern, it should be ignored") {
            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "bad" を "NG word" に置き換えるエイリアス
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "bad",
                    replace = "NG word"
                )
            )

            // "NG word" を含む文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Contains,
                    search = "NG word"
                )
            )

            // ReplacerProcessor で "bad" → "NG word"
            // IgnoreAfterReplaceProcessor で "NG word" を含むので無視される
            val result = processWithAllProcessors(message, "This is bad", voice)
            result.shouldBeNull()
        }
    }

    context("複数のProcessor機能が組み合わさった正常系テスト") {
        // 6. 正常に処理が完了するケース
        test("Multiple processors should work together correctly for valid text") {
            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "hello" を "こんにちは" に置き換えるエイリアス
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "hello",
                    replace = "こんにちは"
                )
            )

            // 別の文字列を無視する設定（"hello" や "こんにちは" には該当しない）
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Equals,
                    search = "ignore this"
                )
            )

            // エイリアスが適用され、無視されないので正常に処理される
            val result = processWithAllProcessors(message, "hello world", voice)
            result.shouldNotBeNull()
            result.first shouldBe "こんにちは world"
        }

        // 7. URL置き換えとエイリアスが両方適用されるケース
        test("URL replacement and alias should both apply when not ignored") {
            mockkObject(UrlReplacer)
            coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

            val message = createSimpleMessageMock(Snowflake(0))
            val voice = Voice(speaker = Speaker.Hikari)

            // "Example Domain" を "例のドメイン" に置き換えるエイリアス
            AliasStore.create(
                AliasData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = AliasType.Text,
                    search = "Example Domain",
                    replace = "例のドメイン"
                )
            )

            // 無関係の文字列を無視する設定
            IgnoreStore.create(
                IgnoreData(
                    guildId = Snowflake(0),
                    userId = Snowflake(123),
                    type = IgnoreType.Equals,
                    search = "completely different text"
                )
            )

            // URL が置き換えられ、さらにエイリアスも適用される
            val result = processWithAllProcessors(message, "Visit https://example.com for info", voice)
            result.shouldNotBeNull()
            result.first shouldBe "Visit Webページ「例のドメイン」へのリンク for info"
        }
    }
})