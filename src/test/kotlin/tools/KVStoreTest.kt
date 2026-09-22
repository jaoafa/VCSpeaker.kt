package tools

import com.jaoafa.vcspeaker.database.tables.KVTable
import com.jaoafa.vcspeaker.tools.kvstore.storedValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import utils.useTables

class KVStoreTest : FunSpec({
    useTables(KVTable)

    test("KVProperty should store and retrieve values correctly") {
        val testObject = object {
            var testValue by storedValue<String>()
        }

        testObject.testValue = "Test String"

        transaction {
            KVTable.selectAll().count() shouldBe 1
            KVTable.selectAll().where { KVTable.key eq "testValue" }.single()[KVTable.value] shouldBe "\"Test String\""
        }

        testObject.testValue shouldBe "Test String"

        testObject.testValue = null

        transaction {
            KVTable.selectAll().count() shouldBe 0
        }
    }
})
