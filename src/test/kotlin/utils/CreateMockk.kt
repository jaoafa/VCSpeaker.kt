package utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

fun createMessageMockk(guildId: Snowflake) = mockk<Message>(relaxed = true).apply {
    coEvery { getGuild() } returns mockk {
        every { id } returns guildId
    }
    every { type } returns dev.kord.common.entity.MessageType.Default
    every { stickers } returns emptyList()
    every { attachments } returns emptySet()
    every { flags } returns null
    every { messageReference } returns null
    every { referencedMessage } returns null
}

fun createGuildMockk(guildId: Snowflake) = mockk<Guild>().apply {
    every { id } returns guildId
}

fun createTextChannelMockk(channelId: Snowflake, guildId: Snowflake) = mockk<TextChannel>().apply {
    every { this@apply.guildId } returns guildId
    every { id } returns channelId
}
