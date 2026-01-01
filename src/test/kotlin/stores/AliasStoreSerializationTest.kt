package stores

import com.jaoafa.vcspeaker.stores.AliasData
import com.jaoafa.vcspeaker.stores.AliasType
import com.jaoafa.vcspeaker.stores.TypedStore
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class AliasStoreSerializationTest : FunSpec({
    test("AliasData ignores unknown keys during deserialization") {
        val base = TypedStore(
            version = 1,
            list = listOf(
                AliasData(
                    guildId = Snowflake(1),
                    userId = Snowflake(2),
                    type = AliasType.Text,
                    search = "hello",
                    replace = "world"
                )
            )
        )

        val encoded = Json.encodeToString(TypedStore.serializer(AliasData.serializer()), base)
        val element = Json.parseToJsonElement(encoded).jsonObject
        val list = element["list"]!!.jsonArray
        val alias = list.first().jsonObject

        val aliasWithExtra = buildJsonObject {
            alias.forEach { (key, value) -> put(key, value) }
            put("soundboard", JsonPrimitive(123))
        }
        val mutated = buildJsonObject {
            element.forEach { (key, value) ->
                if (key == "list") {
                    put("list", JsonArray(listOf(aliasWithExtra)))
                } else {
                    put(key, value)
                }
            }
        }
        val mutatedJson = Json.encodeToString(JsonElement.serializer(), mutated)
        val json = Json {
            ignoreUnknownKeys = true
        }

        val decoded = json.decodeFromString(TypedStore.serializer(AliasData.serializer()), mutatedJson)
        decoded.list.single().replace shouldBe "world"
    }
})
