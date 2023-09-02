package com.jaoafa.vcspeaker.features

import com.jaoafa.vcspeaker.stores.TitleData
import com.jaoafa.vcspeaker.stores.TitleStore
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.VoiceChannel

object Title {

    suspend fun VoiceChannel.setTitle(title: String, user: User): TitleData {
        val data = TitleStore.find(id)

        val latestData = if (data != null) { // update
            TitleStore.remove(data)
            val updatedData = data.copy(title = title, userId = user.id)
            TitleStore.create(updatedData)
            updatedData
        } else { // create
            val newData = TitleData(guild.id, id, user.id, title, name)
            TitleStore.create(newData)
            newData
        }

        edit { name = title }

        return latestData
    }

    suspend fun VoiceChannel.resetTitle(user: User): Pair<TitleData?, TitleData?> {
        val oldData = TitleStore.find(id)

        return if (oldData?.title != null) {
            val newData = oldData.copy(title = null, userId = user.id)
            TitleStore.remove(oldData)
            TitleStore.create(newData)
            edit { name = oldData.original }
            Pair(oldData, newData)
        } else Pair(null, null)
    }

    fun VoiceChannel.getTitleData() = TitleStore.find(id)
}