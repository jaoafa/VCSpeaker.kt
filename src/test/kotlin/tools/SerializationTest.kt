package tools

import com.jaoafa.vcspeaker.tools.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class SerializationTest : FunSpec({
    @Serializable
    data class Person(val name: String, val age: Int)

    val strategy = Person.serializer()
    val tempDir = System.getProperty("java.io.tmpdir") + File.separator + "vcspeaker_test"
    val file = File(tempDir, "person.json")

    beforeTest {
        if (file.exists()) file.delete()
        file.parentFile.mkdirs()
    }

    afterTest {
        if (file.exists()) file.delete()
        file.parentFile?.delete()
    }

    test("readOrCreateAs should create file and return initial context when file does not exist") {
        val context = Person("Alice", 30)
        val result = file.readOrCreateAs(strategy, context) {
            error("should not be called")
        }
        result shouldBe context
        file.readText() shouldBe Json.encodeToString(strategy, context)
    }

    test("readOrCreateAs should read existing file and return deserialized context") {
        val initial = Person("Alice", 30)
        file.writeText(Json.encodeToString(strategy, initial))
        val result = file.readOrCreateAs(strategy, Person("Bob", 40)) {
            Json.decodeFromString(strategy, this)
        }
        result shouldBe initial
    }

    test("writeAs should write file with serialized content") {
        val context = Person("Carol", 25)
        if (file.exists()) file.delete()
        file.parentFile.mkdirs()
        file.writeAs(strategy, context)
        val text = file.readText()
        text shouldBe Json.encodeToString(strategy, context)
    }
})
