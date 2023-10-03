package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.stores.TitleData
import com.jaoafa.vcspeaker.stores.TitleStore
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.VoiceChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Title {

    suspend fun VoiceChannel.setTitle(title: String, user: UserBehavior): TitleData {
        val data = getTitleData()

        val latestData = if (data != null) { // update
            val updatedData = data.copy(
                title = title,
                userId = user.id
            )

            TitleStore.replace(data, updatedData)
        } else { // create
            val newData = TitleData(guild.id, id, user.id, title, name)

            TitleStore.create(newData)
        }

        CoroutineScope(Dispatchers.Default).launch {
            edit { name = title }
        }

        return latestData
    }

    suspend fun VoiceChannel.resetTitle(user: UserBehavior): Pair<TitleData?, TitleData?> {
        val data = getTitleData()

        return if (data?.title != null) {
            val newData = data.copy(
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                edit { name = newData.original }
            }

            data to newData
        } else null to null
    }

    suspend fun VoiceChannel.saveTitle(user: UserBehavior): Pair<TitleData?, TitleData?> {
        val data = getTitleData()

        return if (data?.title != null) {
            val newData = data.copy(
                original = data.title,
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                edit { name = newData.original }
            }

            data to newData
        } else null to null
    }

    suspend fun Guild.saveTitleAll(user: UserBehavior): Map<TitleData, TitleData> {
        val guildTitles = TitleStore.filterGuild(id)

        return guildTitles.filter { it.title != null }.associateWith { data ->
            val newData = data.copy(
                original = data.title!!,
                title = null,
                userId = user.id
            )

            TitleStore.replace(data, newData)

            CoroutineScope(Dispatchers.Default).launch {
                getChannelOf<VoiceChannel>(data.channelId).edit { name = newData.original }
            }

            newData
        }
    }

    fun VoiceChannel.getTitleData() = TitleStore.find(id)
}