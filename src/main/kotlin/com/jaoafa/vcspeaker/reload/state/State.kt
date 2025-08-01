package com.jaoafa.vcspeaker.reload.state

import com.jaoafa.vcspeaker.VCSpeaker
import com.jaoafa.vcspeaker.tts.narrators.NarratorManager
import com.jaoafa.vcspeaker.tts.narrators.NarratorState
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val args: Array<String>,
    val narrators: List<NarratorState>
) {
    companion object {
        fun generate(): State {
            val narratorStates = NarratorManager.list.map { it.prepareTransfer() }
            val args = VCSpeaker.args

            return State(args, narratorStates)
        }
    }
}
