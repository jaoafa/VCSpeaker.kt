package utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

fun FunSpec.createMessageMockk(messageId: Snowflake) = mockk<Message>().apply {
    coEvery { getGuild() } returns mockk {
        every { id } returns messageId
    }
}