package database

import com.jaoafa.vcspeaker.database.DatabaseUtil
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.Constants.TEST_DB_MEM_URL

object TestTableBaseline : IntIdTable("test_table") {
    val normalColumn = varchar("normal_column", 16)
}

object TestTableV1 : IntIdTable("test_table") {
    val normalColumn = varchar("renamed_column", 16)
}

class SchemaMigrationTest : FunSpec({
    test("Flyway migration should run correctly") {
        DatabaseUtil.connect(TEST_DB_MEM_URL)

        transaction {
            SchemaUtils.create(TestTableBaseline)
            SchemaUtils.listTables() shouldBe listOf("PUBLIC.TEST_TABLE")
        }

        // insert with baseline schema (V0)
        val id = transaction {
            val inserted = TestTableBaseline.insert {
                it[normalColumn] = "something"
            }

            return@transaction inserted[TestTableBaseline.id]
        }

        // migrate to V1
        val flyway = Flyway.configure()
            .baselineVersion("0")
            .baselineOnMigrate(true)
            .locations("filesystem:src/test/resources/db/migration")
            .dataSource(TEST_DB_MEM_URL, null, null)
            .load()
        val migration = flyway.migrate()

        migration.migrations.size shouldBe 1

        migration.migrations.first().run {
            version shouldBe "1"
            description shouldBe "rename column"
        }

        // get with V1
        transaction {
            val inserted = TestTableV1.selectAll().where { TestTableV1.id eq id }.single()
            inserted[TestTableV1.normalColumn] shouldBe "something"
        }

        // run migration again (no migrations should run)
        val migration2 = flyway.migrate()
        migration2.migrations.size shouldBe 0
    }
})
