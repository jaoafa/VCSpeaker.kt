package stores

import com.jaoafa.vcspeaker.database.DatabaseUtil
import com.jaoafa.vcspeaker.database.actions.GuildAction.getEntity
import com.jaoafa.vcspeaker.database.actions.ReadableChannelAction.isReadableChannel
import com.jaoafa.vcspeaker.database.onDuplicate
import com.jaoafa.vcspeaker.database.tables.GuildEntity
import com.jaoafa.vcspeaker.database.tables.GuildTable
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.database.transactionResulting
import com.jaoafa.vcspeaker.database.unwrap
import dev.kord.common.entity.Snowflake
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.createGuildMockk
import utils.createTextChannelMockk
import com.jaoafa.vcspeaker.database.tables.ReadableChannelEntity as Entity
import com.jaoafa.vcspeaker.database.tables.ReadableChannelTable as Table

class ReadableChannelStoreTest : FunSpec({
    val guildId1 = Snowflake(111111111111111111UL)
    val guildId2 = Snowflake(222222222222222222UL)
    val channelId1 = Snowflake(333333333333333333UL)
    val channelId2 = Snowflake(444444444444444444UL)
    val creatorId = Snowflake(0UL)

    beforeSpec {
        DatabaseUtil.init("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        DatabaseUtil.createTables()
    }

    beforeEach {
        transaction {
            GuildEntity.new(id = guildId1) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
            GuildEntity.new(id = guildId2) {
                this.speakerVoiceEntity = VoiceEntity.new { }
            }
        }
    }

    afterEach {
        transaction {
            GuildTable.deleteAll()
        }
        clearAllMocks()
    }

    // --- isReadableChannel ---

    context("isReadableChannel") {
        test("If the channel is registered, should return true.") {
            val guild = createGuildMockk(guildId1)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
            }

            channel.isReadableChannel() shouldBe true
        }

        test("If the channel is not registered, should return false.") {
            val channel = createTextChannelMockk(channelId1, guildId1)
            channel.isReadableChannel() shouldBe false
        }

        test("If the guild ID differs even if the channel ID matches, should return false.") {
            val guild = createGuildMockk(guildId1)
            val channel = createTextChannelMockk(channelId1, guildId2)

            transaction {
                Entity.new {
                    guildEntity = guild.getEntity()
                    channelDid = channelId1
                    creatorDid = creatorId
                }
            }

            channel.isReadableChannel() shouldBe false
        }
    }

    // --- add ---

    context("add") {
        test("If a channel is added, the data count should increase.") {
            val guild = createGuildMockk(guildId1)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }

                Entity.all().count() shouldBe 1
                Entity.find { Table.channelDid eq channel.id }.count() shouldBe 1
            }
        }

        test("If the same channel is added twice, the data count should not increase.") {
            val guild = createGuildMockk(guildId1)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
            }

            var duplicate = false

            shouldThrow<ExposedSQLException> {
                transactionResulting {
                    Entity.new {
                        guildEntity = guild.getEntity()
                        channelDid = channel.id
                        creatorDid = creatorId
                    }
                }.onDuplicate {
                    duplicate = true
                }.unwrap()
            }

            duplicate shouldBe true

            transaction {
                Entity.all().count() shouldBe 1
            }
        }

        test("If the same channel ID is added for different guilds, they should be stored as separate entries.") {
            val guild1 = createGuildMockk(guildId1)
            val guild2 = createGuildMockk(guildId2)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild1.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
                Entity.new {
                    guildEntity = guild2.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
            }

            transaction {
                Entity.all().count() shouldBe 2
            }
        }
    }

    // --- remove ---

    context("remove") {
        test("If a channel is removed, the data for that channel should be deleted.") {
            val guild = createGuildMockk(guildId1)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
            }
            transaction {
                Entity.find { (Table.guildDid eq guild.id) and (Table.channelDid eq channel.id) }.single().delete()
            }
            transaction {
                Entity.all().count() shouldBe 0
            }
        }

        test("If a channel is removed, data for the same channel ID in a different guild should not be deleted.") {
            val guild1 = createGuildMockk(guildId1)
            val guild2 = createGuildMockk(guildId2)
            val channel = createTextChannelMockk(channelId1, guildId1)

            transaction {
                Entity.new {
                    guildEntity = guild1.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
                Entity.new {
                    guildEntity = guild2.getEntity()
                    channelDid = channel.id
                    creatorDid = creatorId
                }
            }
            transaction {
                Entity.find { (Table.guildDid eq guild1.id) and (Table.channelDid eq channel.id) }.single().delete()
            }
            transaction {
                Entity.all().count() shouldBe 1
                Entity.find { (Table.guildDid eq guild2.id) and (Table.channelDid eq channel.id) }.count() shouldBe 1
            }
        }

        test("If a channel is removed, data for a different channel in the same guild should not be deleted.") {
            val guild1 = createGuildMockk(guildId1)
            val channel1 = createTextChannelMockk(channelId1, guildId1)
            val channel2 = createTextChannelMockk(channelId2, guildId2)

            transaction {
                Entity.new {
                    guildEntity = guild1.getEntity()
                    channelDid = channel1.id
                    creatorDid = creatorId
                }
                Entity.new {
                    guildEntity = guild1.getEntity()
                    channelDid = channel2.id
                    creatorDid = creatorId
                }
            }

            transaction {
                Entity.find { (Table.guildDid eq guild1.id) and (Table.channelDid eq channel1.id) }.single().delete()
            }

            transaction {
                Entity.all().count() shouldBe 1
                Entity.find { (Table.guildDid eq guild1.id) and (Table.channelDid eq channel2.id) }.count() shouldBe 1
            }
        }
    }
})
