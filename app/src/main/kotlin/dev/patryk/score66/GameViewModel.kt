package dev.patryk.score66

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.GameSession
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.NoOpStorage
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Round
import dev.patryk.score66.data.StateStorage
import dev.patryk.score66.data.formatSessionTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val storage: StateStorage = NoOpStorage
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            storage.load()?.let { _state.value = it }
        }
    }

    private fun mutate(block: (AppState) -> AppState) {
        _state.update(block)
        viewModelScope.launch { storage.save(_state.value) }
    }

    fun selectDeclarer(playerId: Int) = mutate {
        it.copy(pending = PendingRound(declarerId = playerId))
    }

    fun setMultiplier(multiplier: Multiplier) = mutate { s ->
        val current = s.pending ?: return@mutate s
        val next = if (current.multiplier == multiplier) Multiplier.NORMAL else multiplier
        s.copy(pending = current.copy(multiplier = next))
    }

    fun setDeclarerWon(won: Boolean) = mutate { s ->
        val current = s.pending ?: return@mutate s
        s.copy(pending = current.copy(declarerWon = won))
    }

    fun finalizeHand(basePoints: Int) = mutate { s ->
        val pending = s.pending ?: return@mutate s
        val declarerWon = pending.declarerWon ?: return@mutate s
        val round = Round(
            number = s.rounds.size + 1,
            declarerId = pending.declarerId,
            declarerWon = declarerWon,
            basePoints = basePoints,
            multiplier = pending.multiplier
        )
        s.copy(rounds = s.rounds + round, pending = null)
    }

    fun undo() = mutate { s ->
        if (s.rounds.isEmpty()) s else s.copy(rounds = s.rounds.dropLast(1))
    }

    fun newGame() = mutate { s ->
        val history = if (s.rounds.isEmpty()) s.history else {
            val ts = System.currentTimeMillis()
            listOf(
                GameSession(
                    id = ts, savedAt = ts,
                    name = formatSessionTimestamp(ts),
                    players = s.players,
                    rounds = s.rounds
                )
            ) + s.history
        }
        s.copy(rounds = emptyList(), pending = null, history = history)
    }

    fun deleteSession(id: Long) = mutate { s ->
        s.copy(history = s.history.filterNot { it.id == id })
    }

    fun renameSession(id: Long, name: String) = mutate { s ->
        s.copy(history = s.history.map {
            if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it
        })
    }

    fun updatePlayerName(playerId: Int, name: String) = mutate { s ->
        s.copy(players = s.players.map { if (it.id == playerId) it.copy(name = name) else it })
    }

    fun toggleLanguage() = mutate { s ->
        s.copy(language = if (s.language == Language.EN) Language.PL else Language.EN)
    }
}
