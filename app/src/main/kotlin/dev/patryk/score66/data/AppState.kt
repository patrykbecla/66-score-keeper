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
    val multiplier: Multiplier
) {
    val awarded: Int get() = basePoints * multiplier.factor

    fun pointsFor(playerId: Int): Int = when {
        declarerWon && playerId == declarerId -> awarded
        !declarerWon && playerId != declarerId -> awarded
        else -> 0
    }
}

@Serializable
data class PendingRound(
    val declarerId: Int,
    val multiplier: Multiplier = Multiplier.NORMAL,
    val declarerWon: Boolean? = null
)

@Serializable
enum class Language { EN, PL }

@Serializable
data class AppState(
    val players: List<Player> = listOf(
        Player(0, "Player 1"), Player(1, "Player 2"), Player(2, "Player 3")
    ),
    val rounds: List<Round> = emptyList(),
    val pending: PendingRound? = null,
    val language: Language = Language.EN
) {
    fun totalFor(playerId: Int): Int = rounds.sumOf { it.pointsFor(playerId) }
}
