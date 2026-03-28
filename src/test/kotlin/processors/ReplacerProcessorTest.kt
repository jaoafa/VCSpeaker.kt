package processors

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.tables.AliasEntity
import com.jaoafa.vcspeaker.database.tables.AliasTable
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.tts.Voice
import com.jaoafa.vcspeaker.tts.processors.ReplacerProcessor
import com.jaoafa.vcspeaker.tts.providers.voicetext.Speaker
import com.jaoafa.vcspeaker.tts.replacers.UrlReplacer
import dev.kord.common.entity.Snowflake
import dev.kord.core.ClientResources
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.createMessageMockk

/**
 * ReplacerProcessor のテスト (Replacer の複合テスト)
 */
class ReplacerProcessorTest : FunSpec({
    beforeSpec {
        DatabaseUtil.init("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        DatabaseUtil.createTables()

        transaction {
            GuildEntity.new(id = Snowflake(0)) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
        }
    }

    // テスト前にモックを初期化
    beforeTest {
        mockkObject(VCSpeaker)
    }

    // テスト後にモックを削除
    afterTest {
        transaction {
            AliasTable.deleteAll()
        }
        clearAllMocks()
    }

    // テキストが変更されないことを確認
    test("If nothing is configured, the text should remain unchanged.") {
        val text = "Hello, world!"
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)
        processedText shouldBe text
        processedVoice shouldBe voice
    }

    // 複数のエイリアスを設定した場合、正しく置き換えられる
    test("If multiple aliases match the content, the replaced text should be returned.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Text
                search = "Hello"
                replace = "Bonjour"
            }

            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Regex
                search = "w.+d"
                replace = "Kotlin"
            }
        }

        val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

        processedText shouldBe "Bonjour, Kotlin!"
        processedVoice shouldBe voice
    }

    // エイリアスは再帰的には行われない
    test("Alias should not match the content recursively.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Regex
                search = "Hello"
                replace = "Bonjour"
            }
            // should be skipped
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Regex
                search = "Bonjour, world!"
                replace = "你好，Kotlin!"
            }
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Regex
                search = "w.+d"
                replace = "Kotlin"
            }
        }

        val (processedText, processedVoice) = ReplacerProcessor().process(message, "Hello, world!", voice)

        processedText shouldBe "Bonjour, Kotlin!"
        processedVoice shouldBe voice
    }

    // サーバ絵文字のエイリアスがある場合、EmojiReplacer が適用されること
    test("If there is an alias for a server emoji, the EmojiReplacer should be applied.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Emoji
                search = "<:world:123456789012345678>"
                replace = "world"
            }
        }

        val text = "Hello, <:world:123456789012345678>!"
        val expected = "Hello, world!"

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }

    // サーバ絵文字のエイリアスが無い場合、GuildEmojiReplacer が適用されること
    test("If there is no alias for a server emoji, the GuildEmojiReplacer should be applied.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        val text = "Hello, <:world:123456789012345678>!"
        val expected = "Hello, world!"

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }

    // サウンドボードのエイリアスがある場合、SoundboardReplacer が適用されること
    test("If there is a soundboard alias, the SoundboardReplacer should be applied.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Soundboard
                search = "boom"
                replace = "https://cdn.discordapp.com/soundboard-sounds/123456789012345678.mp3"
            }
        }

        val text = "boom!"
        val expected = "<sound:0:123456789012345678>!"

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }

    // サウンドボードのエイリアスがある場合、絵文字置換より先に適用されること
    test("Soundboard alias should be applied before guild emoji replacement.") {
        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Soundboard
                search = "<:godlike_1:1><:godlike_2:2>"
                replace = "1152787936983666768"
            }
        }

        val text = "pre <:godlike_1:1><:godlike_2:2> post"
        val expected = "pre <sound:0:1152787936983666768> post"

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }

    // ユーザメンションはエイリアスでの置き換えができないこと
    test("User mentions shouldn't be replaced by aliases.") {
        every { VCSpeaker.kord } returns mockk {
            every { resources } returns mockk<ClientResources>() // kordをmock化するために必要
            coEvery { getGuildOrNull(Snowflake(0)) } returns mockk {
                coEvery { getMember(Snowflake(123456789012345678)) } returns mockk {
                    every { effectiveName } returns "test-user" // テスト用のユーザー名
                }
            }
        }

        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Text
                search = "test-user"
                replace = "テストユーザー"
            }
        }

        val text = "Hello, <@123456789012345678>!"
        val expected = "Hello, @test-user!" // メンションは置き換えられない

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }

    // URL が置き換えられたあと、エイリアスは適用されること
    test("After the URL is replaced, the alias should be applied if any.") {
        mockkObject(UrlReplacer)
        coEvery { UrlReplacer["getPageTitle"]("https://example.com") } returns "Example Domain"

        val message = createMessageMockk(Snowflake(0))
        val voice = Voice(speaker = Speaker.Hikari)

        transaction {
            AliasEntity.new {
                guildEntity = GuildEntity.findById(Snowflake(0))!!
                creatorDid = Snowflake(1)
                type = AliasType.Text
                search = "Example Domain"
                replace = "例のドメイン"
            }
        }

        val text = "Please visit https://example.com for more information."
        val expected = "Please visit Webページ「例のドメイン」へのリンク for more information."

        val (processedText, processedVoice) = ReplacerProcessor().process(message, text, voice)

        processedText shouldBe expected
        processedVoice shouldBe voice
    }
})
