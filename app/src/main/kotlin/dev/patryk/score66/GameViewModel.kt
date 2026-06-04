package dev.patryk.score66

import androidx.lifecycle.ViewModel
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun selectDeclarer(playerId: Int) {
        _state.update { it.copy(pending = PendingRound(declarerId = playerId)) }
    }

    fun setMultiplier(multiplier: Multiplier) {
        _state.update { s ->
            val current = s.pending ?: return@update s
            val next = if (current.multiplier == multiplier) Multiplier.NORMAL else multiplier
            s.copy(pending = current.copy(multiplier = next))
        }
    }

    fun setDeclarerWon(won: Boolean) {
        _state.update { s ->
            val current = s.pending ?: return@update s
            s.copy(pending = current.copy(declarerWon = won))
        }
    }

    fun finalizeHand(basePoints: Int) {
        _state.update { s ->
            val pending = s.pending ?: return@update s
            val declarerWon = pending.declarerWon ?: return@update s
            val round = Round(
                number = s.rounds.size + 1,
                declarerId = pending.declarerId,
                declarerWon = declarerWon,
                basePoints = basePoints,
                multiplier = pending.multiplier
            )
            s.copy(rounds = s.rounds + round, pending = null)
        }
    }

    fun undo() {
        _state.update { s ->
            if (s.rounds.isEmpty()) s else s.copy(rounds = s.rounds.dropLast(1))
        }
    }

    fun newGame() {
        _state.update { s -> s.copy(rounds = emptyList(), pending = null) }
    }

    fun updatePlayerName(playerId: Int, name: String) {
        _state.update { s ->
            s.copy(players = s.players.map { if (it.id == playerId) it.copy(name = name) else it })
        }
    }

    fun toggleLanguage() {
        _state.update { s ->
            s.copy(language = if (s.language == Language.EN) Language.PL else Language.EN)
        }
    }
}
