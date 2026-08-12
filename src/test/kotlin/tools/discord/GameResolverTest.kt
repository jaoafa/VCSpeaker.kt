package tools.discord

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.stores.GameData
import com.jaoafa.vcspeaker.stores.GameStore
import com.jaoafa.vcspeaker.tools.discord.DiscordGameApi
import com.jaoafa.vcspeaker.tools.discord.GameResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class GameResolverTest : FunSpec({
    beforeSpec {
        mockkObject(VCSpeaker)
        every { VCSpeaker.storeFolder } returns File(System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker-test-${System.currentTimeMillis()}")
        VCSpeaker.storeFolder.mkdirs()

        val gameFile = File(VCSpeaker.storeFolder, "games.json")
        gameFile.writeText("""{"version":1,"list":[]}""")
    }

    afterTest {
        clearAllMocks()
        GameStore.data.clear()
    }

    test("If the catalog is fresh, DiscordGameApi should not be called.") {
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns System.currentTimeMillis()
        coEvery { GameStore.find(1L) } returns GameData(1L, "Game One", System.currentTimeMillis())

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One")
        coVerify(exactly = 0) { DiscordGameApi.getDetectableGames() }
    }

    test("If the catalog has never been fetched, DiscordGameApi should be called.") {
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns null
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One")
        coEvery { GameStore.replaceAll(any(), any()) } returns Unit
        coEvery { GameStore.find(1L) } returns GameData(1L, "Game One", System.currentTimeMillis())

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }

    test("If the catalog is stale, DiscordGameApi should be called and the store should be replaced.") {
        val staleTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns staleTimestamp
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One (Updated)")
        coEvery { GameStore.replaceAll(any(), any()) } returns Unit
        coEvery { GameStore.find(1L) } returns GameData(1L, "Game One (Updated)", System.currentTimeMillis())

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One (Updated)")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
        coVerify(exactly = 1) { GameStore.replaceAll(mapOf(1L to "Game One (Updated)"), any()) }
    }

    test("If the refresh fails and the id exists in the stale store, the stale name should be returned.") {
        val staleTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns staleTimestamp
        coEvery { DiscordGameApi.getDetectableGames() } returns null
        coEvery { GameStore.find(1L) } returns GameData(1L, "Old Game Name", staleTimestamp)

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Old Game Name")
        coVerify(exactly = 0) { GameStore.replaceAll(any(), any()) }
    }

    test("If the refresh fails and the id does not exist in the store, it should be unresolved.") {
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns null
        coEvery { DiscordGameApi.getDetectableGames() } returns null
        coEvery { GameStore.find(999L) } returns null

        val result = GameResolver.resolve(listOf(999L))

        result shouldBe mapOf(999L to null)
    }

    test("If multiple ids are requested, the catalog should be refreshed only once.") {
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        coEvery { GameStore.lastFetchedAt() } returns null
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One", 2L to "Game Two")
        coEvery { GameStore.replaceAll(any(), any()) } returns Unit
        coEvery { GameStore.find(1L) } returns GameData(1L, "Game One", System.currentTimeMillis())
        coEvery { GameStore.find(2L) } returns GameData(2L, "Game Two", System.currentTimeMillis())

        val result = GameResolver.resolve(listOf(1L, 2L))

        result shouldBe mapOf(1L to "Game One", 2L to "Game Two")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }

    test("If resolve is called concurrently while the catalog is stale, the refresh should still happen only once.") {
        mockkObject(GameStore)
        mockkObject(DiscordGameApi)
        var lastFetchedAt: Long? = null
        coEvery { GameStore.lastFetchedAt() } answers { lastFetchedAt }
        coEvery { DiscordGameApi.getDetectableGames() } coAnswers {
            delay(50)
            mapOf(1L to "Game One", 2L to "Game Two")
        }
        coEvery { GameStore.replaceAll(any(), any()) } coAnswers {
            lastFetchedAt = secondArg<Long>()
        }
        coEvery { GameStore.find(1L) } returns GameData(1L, "Game One", System.currentTimeMillis())
        coEvery { GameStore.find(2L) } returns GameData(2L, "Game Two", System.currentTimeMillis())

        coroutineScope {
            val d1 = async { GameResolver.resolve(listOf(1L)) }
            val d2 = async { GameResolver.resolve(listOf(2L)) }
            d1.await()
            d2.await()
        }

        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }
})
