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
        test("If the channel is registered, should return true.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.isReadableChannel(guildId1, mockChannel(channelId1)) shouldBe true
        }

        test("If the channel is not registered, should return false.") {
            ReadableChannelStore.isReadableChannel(guildId1, mockChannel(channelId1)) shouldBe false
        }

        test("If the guild ID differs even if the channel ID matches, should return false.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.isReadableChannel(guildId2, mockChannel(channelId1)) shouldBe false
        }
    }

    // --- add ---

    context("add") {
        test("If a channel is added, the data count should increase.") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().channelId shouldBe channelId1
        }

        test("If the same channel is added twice, the data count should not increase.") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 1
        }

        test("If the same channel ID is added for different guilds, they should be stored as separate entries.") {
            ReadableChannelStore.add(guildId1, mockChannel(channelId1), addedByUserId)
            ReadableChannelStore.add(guildId2, mockChannel(channelId1), addedByUserId)

            ReadableChannelStore.data shouldHaveSize 2
        }
    }

    // --- remove ---

    context("remove") {
        test("If a channel is removed, the data for that channel should be deleted.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 0
        }

        test("If a channel is removed, data for the same channel ID in a different guild should not be deleted.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId2, channelId1, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().guildId shouldBe guildId2
        }

        test("If a channel is removed, data for a different channel in the same guild should not be deleted.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId2, addedByUserId))

            ReadableChannelStore.remove(guildId1, mockChannel(channelId1))

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().channelId shouldBe channelId2
        }
    }

    // --- removeForGuild ---

    context("removeForGuild") {
        test("If removeForGuild is called, all data for the specified guild should be deleted.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId2, addedByUserId))

            ReadableChannelStore.removeForGuild(guildId1)

            ReadableChannelStore.data shouldHaveSize 0
        }

        test("If removeForGuild is called, data for a different guild should not be deleted.") {
            ReadableChannelStore.data.add(ReadableChannelData(guildId1, channelId1, addedByUserId))
            ReadableChannelStore.data.add(ReadableChannelData(guildId2, channelId1, addedByUserId))

            ReadableChannelStore.removeForGuild(guildId1)

            ReadableChannelStore.data shouldHaveSize 1
            ReadableChannelStore.data.first().guildId shouldBe guildId2
        }
    }
})
