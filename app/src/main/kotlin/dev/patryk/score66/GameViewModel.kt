package dev.patryk.score66

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.GameSession
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.NoOpStorage
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Player
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
            storage.load()?.let { loaded ->
                // Pad to 4 players so 4P mode is always available after loading old saves
                val players = if (loaded.players.size < 4) {
                    loaded.players + (loaded.players.size until 4).map { i ->
                        Player(i, "Player ${i + 1}")
                    }
                } else loaded.players
                _state.value = loaded.copy(players = players)
            }
        }
    }

    private fun mutate(block: (AppState) -> AppState) {
        _state.update(block)
        viewModelScope.launch { storage.save(_state.value) }
    }

    fun selectDeclarer(playerId: Int) = mutate { s ->
        val current = s.pending
        val newWatcherId = if (current?.watcherId == playerId) null else current?.watcherId
        s.copy(pending = PendingRound(declarerId = playerId, watcherId = newWatcherId))
    }

    fun setWatcher(id: Int) = mutate { s ->
        val current = s.pending
        val newWatcherId = if (current?.watcherId == id) null else id
        val newDeclarerId = if (current?.declarerId == id) null else current?.declarerId
        if (newWatcherId == null && newDeclarerId == null) {
            s.copy(pending = null)
        } else {
            s.copy(pending = PendingRound(
                declarerId = newDeclarerId,
                watcherId = newWatcherId,
                multiplier = current?.multiplier ?: Multiplier.NORMAL,
                declarerWon = current?.declarerWon
            ))
        }
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
        val declarerId = pending.declarerId ?: return@mutate s
        val declarerWon = pending.declarerWon ?: return@mutate s
        val round = Round(
            number = s.rounds.size + 1,
            declarerId = declarerId,
            declarerWon = declarerWon,
            basePoints = basePoints,
            multiplier = pending.multiplier,
            watcherId = pending.watcherId,
            numPlayers = s.activePlayers.size
        )
        val nextPending = pending.watcherId?.let { PendingRound(watcherId = it) }
        s.copy(rounds = s.rounds + round, pending = nextPending)
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
                    players = s.activePlayers,
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

    fun togglePlayerMode() = mutate { s ->
        val newFourPlayer = !s.fourPlayer
        // If switching to 3P, clear pending if it references player 3
        val newPending = if (!newFourPlayer) {
            s.pending?.let { p ->
                if (p.declarerId == 3) null else p.copy(watcherId = null)
            }
        } else s.pending
        s.copy(fourPlayer = newFourPlayer, pending = newPending)
    }
}
