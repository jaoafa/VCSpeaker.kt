package com.jaoafa.vcspeaker.state

import com.jaoafa.vcspeaker.tts.narrators.NarratorManager
import com.jaoafa.vcspeaker.tts.narrators.NarratorState
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val narrators: List<NarratorState>
) {
    companion object {
        fun generate(): State {
            val narratorStates = NarratorManager.list.map { it.prepareState() }

            return State(narratorStates)
        }
    }
}