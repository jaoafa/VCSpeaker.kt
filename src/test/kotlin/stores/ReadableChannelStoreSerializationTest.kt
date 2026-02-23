package stores

import com.jaoafa.vcspeaker.stores.ReadableChannelData
import dev.kord.common.entity.Snowflake
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

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
})
