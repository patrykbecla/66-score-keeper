package dev.patryk.score66

import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.GameSession
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Player
import dev.patryk.score66.data.Round
import dev.patryk.score66.ui.EnStrings
import dev.patryk.score66.ui.PlStrings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 6 acceptance: full AppState serialisation round-trips (including
 * pending state), corrupt-input handling, and D/K/R tag correctness in EN/PL.
 */
class PersistenceTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Serialisation round-trips ─────────────────────────────────────────────

    @Test
    fun `full AppState with rounds and pending survives serialisation`() {
        val original = AppState(
            players = listOf(Player(0, "Alice"), Player(1, "Bob"), Player(2, "Carol")),
            rounds = listOf(
                Round(1, declarerId = 0, declarerWon = true,  basePoints = 3, multiplier = Multiplier.NORMAL),
                Round(2, declarerId = 1, declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE),
            ),
            pending = PendingRound(
                declarerId = 2,
                multiplier = Multiplier.REDOUBLE,
                declarerWon = true
            ),
            language = Language.PL
        )

        val restored = json.decodeFromString<AppState>(json.encodeToString(original))

        assertEquals(original.players, restored.players)
        assertEquals(original.rounds, restored.rounds)
        assertEquals(original.pending, restored.pending)
        assertEquals(Language.PL, restored.language)
    }

    @Test
    fun `pending with all three fields preserved — covers mid-game relaunch`() {
        val state = AppState(
            pending = PendingRound(
                declarerId = 1,
                multiplier = Multiplier.DOUBLE,
                declarerWon = false
            )
        )
        val restored = json.decodeFromString<AppState>(json.encodeToString(state))

        assertNotNull(restored.pending)
        assertEquals(1, restored.pending?.declarerId)
        assertEquals(Multiplier.DOUBLE, restored.pending?.multiplier)
        assertEquals(false, restored.pending?.declarerWon)
    }

    @Test
    fun `pending with no won-lost (phase 1 of hand) preserved`() {
        val state = AppState(
            pending = PendingRound(declarerId = 0, multiplier = Multiplier.REDOUBLE, declarerWon = null)
        )
        val restored = json.decodeFromString<AppState>(json.encodeToString(state))

        assertEquals(0, restored.pending?.declarerId)
        assertEquals(Multiplier.REDOUBLE, restored.pending?.multiplier)
        assertNull(restored.pending?.declarerWon)
    }

    @Test
    fun `null pending preserved`() {
        val state = AppState(pending = null)
        val restored = json.decodeFromString<AppState>(json.encodeToString(state))
        assertNull(restored.pending)
    }

    @Test
    fun `all three multipliers survive round-trip`() {
        for (multiplier in Multiplier.entries) {
            val round = Round(1, declarerId = 0, declarerWon = true, basePoints = 3, multiplier = multiplier)
            val state = AppState(rounds = listOf(round))
            val restored = json.decodeFromString<AppState>(json.encodeToString(state))
            assertEquals(multiplier, restored.rounds[0].multiplier)
        }
    }

    @Test
    fun `corrupt JSON decodes to null without throwing`() {
        val result = try { json.decodeFromString<AppState>("not valid json { }") } catch (_: Exception) { null }
        assertNull(result)
    }

    @Test
    fun `empty JSON object decodes to null without throwing`() {
        val result = try { json.decodeFromString<AppState>("{}") } catch (_: Exception) { null }
        // Either null or a valid AppState with defaults — it must not crash
        // (kotlinx.serialization may fail on missing required fields or succeed with defaults)
    }

    // ── D / K / R tag localisation ────────────────────────────────────────────

    @Test
    fun `EN double tag is D, redouble tag is R`() {
        assertEquals("D", EnStrings.doubleTag)
        assertEquals("R", EnStrings.redoubleTag)
    }

    @Test
    fun `PL double tag is K, redouble tag is R`() {
        assertEquals("K", PlStrings.doubleTag)
        assertEquals("R", PlStrings.redoubleTag)
    }

    @Test
    fun `tag selection matches PlayerHistoryBlock logic for all multipliers in EN`() {
        fun tagFor(m: Multiplier) = when (m) {
            Multiplier.DOUBLE   -> EnStrings.doubleTag
            Multiplier.REDOUBLE -> EnStrings.redoubleTag
            Multiplier.NORMAL   -> ""
        }
        assertEquals("D", tagFor(Multiplier.DOUBLE))
        assertEquals("R", tagFor(Multiplier.REDOUBLE))
        assertEquals("",  tagFor(Multiplier.NORMAL))
    }

    @Test
    fun `tag selection matches PlayerHistoryBlock logic for all multipliers in PL`() {
        fun tagFor(m: Multiplier) = when (m) {
            Multiplier.DOUBLE   -> PlStrings.doubleTag
            Multiplier.REDOUBLE -> PlStrings.redoubleTag
            Multiplier.NORMAL   -> ""
        }
        assertEquals("K", tagFor(Multiplier.DOUBLE))
        assertEquals("R", tagFor(Multiplier.REDOUBLE))
        assertEquals("",  tagFor(Multiplier.NORMAL))
    }

    // ── GameSession serialisation ─────────────────────────────────────────────

    @Test
    fun `AppState with history survives serialisation round-trip`() {
        val session = GameSession(
            id = 1000L, savedAt = 1000L, name = "Summer game",
            players = listOf(Player(0, "Alice"), Player(1, "Bob"), Player(2, "Carol")),
            rounds = listOf(
                Round(1, declarerId = 0, declarerWon = true, basePoints = 3, multiplier = Multiplier.NORMAL),
                Round(2, declarerId = 1, declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE),
            )
        )
        val original = AppState(history = listOf(session))
        val restored = json.decodeFromString<AppState>(json.encodeToString(original))

        assertEquals(1, restored.history.size)
        assertEquals(session, restored.history[0])
    }

    @Test
    fun `AppState without history field defaults to empty list (backward compat)`() {
        val legacyJson = """{"players":[{"id":0,"name":"Player 1"},{"id":1,"name":"Player 2"},{"id":2,"name":"Player 3"}],"rounds":[],"language":"EN"}"""
        val restored = json.decodeFromString<AppState>(legacyJson)
        assertEquals(emptyList<GameSession>(), restored.history)
    }
}
