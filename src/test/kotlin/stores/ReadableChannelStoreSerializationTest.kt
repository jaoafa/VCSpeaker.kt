package stores

import com.jaoafa.vcspeaker.stores.ReadableChannelData
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

class ReadableChannelStoreSerializationTest : FunSpec({
    test("ReadableChannelData ignores unknown keys during deserialization") {
        val json = """
            {
                "guildId": "123456789012345678",
                "channelId": "987654321098765432",
                "addedByUserId": "111222333444555666",
                "unknownKey": "This should be ignored"
            }
        """.trimIndent()

        val jsonParser = Json { ignoreUnknownKeys = true }
        val data = jsonParser.decodeFromString<ReadableChannelData>(json)

        data.guildId shouldBe Snowflake(123456789012345678UL)
        data.channelId shouldBe Snowflake(987654321098765432UL)
        data.addedByUserId shouldBe Snowflake(111222333444555666UL)
    }

    test("ReadableChannelData serializes and deserializes correctly via TypedStore") {
        val base = TypedStore(
            version = 1,
            list = listOf(
                ReadableChannelData(
                    guildId = Snowflake(1UL),
                    channelId = Snowflake(2UL),
                    addedByUserId = Snowflake(3UL)
                )
            )
        )

        val encoded = Json.encodeToString(TypedStore.serializer(ReadableChannelData.serializer()), base)
        val element = Json.parseToJsonElement(encoded).jsonObject
        val list = element["list"]!!.jsonArray
        val item = list.first().jsonObject

        // 未知のキーを追加してデシリアライズできることを確認
        val itemWithExtra = buildJsonObject {
            item.forEach { (key, value) -> put(key, value) }
            put("extraKey", JsonPrimitive("extraValue"))
        }
        val mutated = buildJsonObject {
            element.forEach { (key, value) ->
                if (key == "list") {
                    put("list", JsonArray(listOf(itemWithExtra)))
                } else {
                    put(key, value)
                }
            }
        }
        val mutatedJson = Json.encodeToString(JsonElement.serializer(), mutated)
        val json = Json { ignoreUnknownKeys = true }

        val decoded = json.decodeFromString(TypedStore.serializer(ReadableChannelData.serializer()), mutatedJson)
        decoded.list.single().channelId shouldBe Snowflake(2UL)
        decoded.list.single().guildId shouldBe Snowflake(1UL)
        decoded.list.single().addedByUserId shouldBe Snowflake(3UL)
    }
})
