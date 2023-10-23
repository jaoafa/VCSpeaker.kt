package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.stores.TitleData
import com.jaoafa.vcspeaker.stores.TitleStore
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.name
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.channel.StageChannel
import dev.kord.core.entity.channel.VoiceChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Title {
    suspend fun BaseVoiceChannelBehavior.setTitle(title: String, user: UserBehavior): TitleData {
        val data = getTitleData()

        val latestData = if (data != null) { // update
            val updatedData = data.copy(
                title = title,
                userId = user.id
            )

            TitleStore.replace(data, updatedData)
        } else { // create
            val newData = TitleData(guild.id, id, user.id, title, this.asChannel().name)

            TitleStore.create(newData)
        }

        CoroutineScope(Dispatchers.Default).launch {
            when (this@setTitle) {
                is VoiceChannel -> edit { name = title }
                is StageChannel -> edit { name = title }
            }
        }

        return latestData
    }

    suspend fun BaseVoiceChannelBehavior.resetTitle(user: UserBehavior): Pair<TitleData?, TitleData?> {
        val data = getTitleData()

        return if (data?.title != null) {
            val newData = data.copy(
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                when (this@resetTitle) {
                    is VoiceChannel -> edit { name = newData.original }
                    is StageChannel -> edit { name = newData.original }
                }
            }

            data to newData
        } else null to null
    }

    suspend fun BaseVoiceChannelBehavior.saveTitle(user: UserBehavior): Pair<TitleData?, TitleData?> {
        val data = getTitleData()

        return if (data != null) {
            val newData = data.copy(
                original = name(),
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                when (this@saveTitle) {
                    is VoiceChannel -> edit { name = newData.original }
                    is StageChannel -> edit { name = newData.original }
                }
            }

            data to newData
        } else null to null
    }

    suspend fun GuildBehavior.saveTitleAll(user: UserBehavior): Map<TitleData, TitleData> {
        val guildTitles = TitleStore.filterGuild(id)

        return guildTitles.associateWith { data ->
            val newData = data.copy(
                original = getChannel(data.channelId).name,
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                when (val channel = getChannel(data.channelId)) {
                    is VoiceChannel -> channel.edit { name = newData.original }
                    is StageChannel -> channel.edit { name = newData.original }
                }
            }

            newData
        }
    }

    fun BaseVoiceChannelBehavior.getTitleData() = TitleStore.find(id)
}