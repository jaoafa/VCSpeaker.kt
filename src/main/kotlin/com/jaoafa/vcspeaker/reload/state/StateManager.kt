package com.jaoafa.vcspeaker.reload.state

import com.jaoafa.vcspeaker.KordStarter.launch
import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.api.Server
import com.jaoafa.vcspeaker.api.ServerType
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StateManager {
    private val logger = KotlinLogging.logger { }

    /**
     * Latest -> Locked from the start
     * Current -> Locked when the transfer happens
     */
    var locked = Server.type == ServerType.Latest
        private set

    fun lock() {
        locked = true
    }

    fun prepareTransfer(): State {
        locked = true
        return State.generate()
    }

    private var reconnector: (suspend () -> Unit)? = null

    /**
     * [State] を受け取り、インスタンスの状態を復元します。
     *
     * @param state 復元する [State]
     *
     * @return ログイン後の状態を復元する関数
     */
    fun restore(state: State) {
        val connectors = state.narrators.map {
            Triple(it.guildId, it.channelId, NarratorManager.prepareAdd(it.guildId, it.channelId))
        }

        reconnector = {
            connectors.forEach { (guildId, channelId, connector) ->
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        connector.invoke()
                    } catch (e: Exception) {
                        logger.error(e) { "Reconnection failed for ${channelId} at ${guildId}" }
                    }
                }
            }
        }
    }

    /**
     * ログイン状態を復元します。
     */
    suspend fun reconnect() {
        logger.info { "Reconnecting to the voice channels..." }

        reconnector?.invoke()
        reconnector = null
    }
}