package tools.discord

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.tables.GameEntity
import com.jaoafa.vcspeaker.database.tables.GameTable
import com.jaoafa.vcspeaker.tools.discord.DiscordGameApi
import com.jaoafa.vcspeaker.tools.discord.GameResolver
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL
import kotlin.time.Duration.Companion.milliseconds

class GameResolverTest : FunSpec({
    beforeSpec {
        DatabaseUtil.connect(TEST_DB_MEM_URL)
        DatabaseUtil.createTables()
    }

    afterEach {
        clearAllMocks()
        transaction {
            GameTable.deleteAll()
        }

        val refreshFailedUntilField = GameResolver::class.java.getDeclaredField("refreshFailedUntil")
        refreshFailedUntilField.isAccessible = true
        refreshFailedUntilField.set(GameResolver, null)

        val lastFetchedAtField = GameResolver::class.java.getDeclaredField("lastFetchedAt")
        lastFetchedAtField.isAccessible = true
        lastFetchedAtField.set(GameResolver, null)
    }

    test("If the catalog is fresh, DiscordGameApi should not be called.") {
        mockkObject(DiscordGameApi)
        GameResolver.setFetchTimestamp(System.currentTimeMillis())
        transaction {
            GameEntity.new(Snowflake(1L)) {
                name = "Game One"
            }
        }

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One")
        coVerify(exactly = 0) { DiscordGameApi.getDetectableGames() }
    }

    test("If the catalog has never been fetched, DiscordGameApi should be called.") {
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One")

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }

    test("If the catalog is stale, DiscordGameApi should be called and the database rows should be replaced.") {
        val staleTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
        GameResolver.setFetchTimestamp(staleTimestamp)
        transaction {
            GameEntity.new(Snowflake(1L)) {
                name = "Game One (Old)"
            }
        }
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One (Updated)")

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Game One (Updated)")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
        transaction {
            GameEntity.findById(Snowflake(1L))?.name shouldBe "Game One (Updated)"
        }
    }

    test("If the refresh fails and the id exists in the stale database, the stale name should be returned.") {
        val staleTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
        GameResolver.setFetchTimestamp(staleTimestamp)
        transaction {
            GameEntity.new(Snowflake(1L)) {
                name = "Old Game Name"
            }
        }
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns null

        val result = GameResolver.resolve(listOf(1L))

        result shouldBe mapOf(1L to "Old Game Name")
    }

    test("If the refresh fails and the id does not exist in the database, it should be unresolved.") {
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns null

        val result = GameResolver.resolve(listOf(999L))

        result shouldBe emptyMap()
    }

    test("If multiple ids are requested, the catalog should be refreshed only once.") {
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns mapOf(1L to "Game One", 2L to "Game Two")

        val result = GameResolver.resolve(listOf(1L, 2L))

        result shouldBe mapOf(1L to "Game One", 2L to "Game Two")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }

    test("If resolve is called concurrently while the catalog is stale, the refresh should still happen only once.") {
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } coAnswers {
            delay(50.milliseconds)
            mapOf(1L to "Game One", 2L to "Game Two")
        }

        val (r1, r2) = coroutineScope {
            val d1 = async { GameResolver.resolve(listOf(1L)) }
            val d2 = async { GameResolver.resolve(listOf(2L)) }
            d1.await() to d2.await()
        }

        r1 shouldBe mapOf(1L to "Game One")
        r2 shouldBe mapOf(2L to "Game Two")
        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }

    test("If a refresh fails, the next resolve within the cooldown window should not retry the fetch.") {
        mockkObject(DiscordGameApi)
        coEvery { DiscordGameApi.getDetectableGames() } returns null

        GameResolver.resolve(listOf(1L))
        GameResolver.resolve(listOf(1L))

        coVerify(exactly = 1) { DiscordGameApi.getDetectableGames() }
    }
})
