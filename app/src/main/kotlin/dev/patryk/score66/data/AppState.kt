package dev.patryk.score66.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String
)

@Serializable
enum class Multiplier(val factor: Int) {
    NORMAL(1), DOUBLE(2), REDOUBLE(4)
}

@Serializable
data class Round(
    val number: Int,
    val declarerId: Int,
    val declarerWon: Boolean,
    val basePoints: Int,
    val multiplier: Multiplier,
    val watcherId: Int? = null,
    val numPlayers: Int? = null
) {
    val awarded: Int get() = basePoints * multiplier.factor

    fun pointsFor(playerId: Int): Int {
        val activePlayers = numPlayers ?: 3
        if (playerId >= activePlayers) return 0
        if (playerId == watcherId) return 0
        return when {
            declarerWon && playerId == declarerId -> awarded
            !declarerWon && playerId != declarerId -> awarded
            else -> 0
        }
    }
}

@Serializable
data class PendingRound(
    val declarerId: Int? = null,
    val watcherId: Int? = null,
    val multiplier: Multiplier = Multiplier.NORMAL,
    val declarerWon: Boolean? = null
)

@Serializable
enum class Language { EN, PL }

@Serializable
data class GameSession(
    val id: Long,
    val savedAt: Long,
    val name: String,
    val players: List<Player>,
    val rounds: List<Round>
)

@Serializable
data class AppState(
    val players: List<Player> = listOf(
        Player(0, "Player 1"), Player(1, "Player 2"), Player(2, "Player 3"), Player(3, "Player 4")
    ),
    val rounds: List<Round> = emptyList(),
    val pending: PendingRound? = null,
    val language: Language = Language.EN,
    val history: List<GameSession> = emptyList(),
    val fourPlayer: Boolean = false
) {
    val activePlayers: List<Player> get() = players.take(if (fourPlayer) 4 else 3)

    fun totalFor(playerId: Int): Int = rounds.sumOf { it.pointsFor(playerId) }

    fun cumulativeSeriesFor(playerId: Int): List<Int> {
        var acc = 0
        return buildList {
            add(0)
            rounds.forEach { acc += it.pointsFor(playerId); add(acc) }
        }
    }
}
