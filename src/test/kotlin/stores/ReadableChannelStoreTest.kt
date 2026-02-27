package stores

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.ReadableChannelData
import com.jaoafa.vcspeaker.stores.ReadableChannelStore
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.TextChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class ReadableChannelStoreTest : FunSpec({
    val guildId1 = Snowflake(111111111111111111UL)
    val guildId2 = Snowflake(222222222222222222UL)
    val channelId1 = Snowflake(333333333333333333UL)
    val channelId2 = Snowflake(444444444444444444UL)
    val addedByUserId = Snowflake(0UL)

    /**
     * 指定した ID を持つ TextChannel のモックを生成します。
     */
    fun mockChannel(channelId: Snowflake) = mockk<TextChannel> {
        every { id } returns channelId
    }

    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker")
        VCSpeaker.storeFolder.mkdirs()
        // ReadableChannelStore 初期化用の空ファイルを作成（未作成の場合のみ）
        val readableChannelFile = File(VCSpeaker.storeFolder, "readablechannels.json")
        if (!readableChannelFile.exists()) {
            readableChannelFile.writeText("[]")
        }
    }

    afterTest {
        ReadableChannelStore.data.clear()
    }

    // --- isReadableChannel ---

    context("isReadableChannel") {
        test("登録済みのチャンネルは true を返す") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.isReadableChannel(guildId1, mockChannel(channelId1)) shouldBe true
        }

        test("未登録のチャンネルは false を返す") {
            ReadableChannelStore.isReadableChannel(guildId1, mockChannel(channelId1)) shouldBe false
        }

        test("チャンネル ID が一致してもギルド ID が異なる場合は false を返す") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.isReadableChannel(guildId2, mockChannel(channelId1)) shouldBe false
        }
    }

    // --- add ---

    context("add") {
        test("チャンネルを追加するとデータが増える") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().channelId shouldBe channelId1
        }

        test("同一チャンネルを重複して追加してもデータが増えない") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 1
        }

        test("異なるギルドの同一チャンネル ID は別エントリとして追加される") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)
            ReadableChannelStore.add(guildId2, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 2
        }
    }

    // --- remove ---

    context("remove") {
        test("指定したチャンネルのデータが削除される") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 0
        }

        test("同一チャンネル ID でもギルドが異なるデータは削除されない") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId2, channelId1, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().guildId shouldBe guildId2
        }

        test("同一ギルドでも別チャンネルのデータは削除されない") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId2, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().channelId shouldBe channelId2
        }
    }

    // --- removeForGuild ---

    context("removeForGuild") {
        test("指定したギルドの全データが削除される") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId2, addedByUserId))

            ReadableChannelStore.removeForGuild(guildId1)

            ReadableChannelStore.data shouldHaveSize 0
        }

        test("異なるギルドのデータは削除されない") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId2, channelId1, addedByUserId))

            ReadableChannelStore.removeForGuild(guildId1)

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().guildId shouldBe guildId2
        }
    }
})
