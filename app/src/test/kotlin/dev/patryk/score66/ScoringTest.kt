package dev.patryk.score66

import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.Round
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 18 scoring cases: 3 basePoints × 3 multipliers × 2 outcomes (win/lose).
 * Declarer is always player 0; opponents are players 1 and 2.
 */
class ScoringTest {

    private fun round(
        declarerWon: Boolean,
        basePoints: Int,
        multiplier: Multiplier,
        declarerId: Int = 0
    ) = Round(
        number = 1,
        declarerId = declarerId,
        declarerWon = declarerWon,
        basePoints = basePoints,
        multiplier = multiplier
    )

    private fun totals(vararg rounds: Round): Triple<Int, Int, Int> {
        val state = AppState(rounds = rounds.toList())
        return Triple(state.totalFor(0), state.totalFor(1), state.totalFor(2))
    }

    // ── Declarer wins ────────────────────────────────────────────────────────

    @Test fun `win noTrick normal scores 3 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 3, multiplier = Multiplier.NORMAL))
        assertEquals(3, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withTrick normal scores 2 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 2, multiplier = Multiplier.NORMAL))
        assertEquals(2, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withHalf normal scores 1 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 1, multiplier = Multiplier.NORMAL))
        assertEquals(1, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win noTrick double scores 6 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 3, multiplier = Multiplier.DOUBLE))
        assertEquals(6, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withTrick double scores 4 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 2, multiplier = Multiplier.DOUBLE))
        assertEquals(4, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withHalf double scores 2 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 1, multiplier = Multiplier.DOUBLE))
        assertEquals(2, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win noTrick redouble scores 12 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 3, multiplier = Multiplier.REDOUBLE))
        assertEquals(12, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withTrick redouble scores 8 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 2, multiplier = Multiplier.REDOUBLE))
        assertEquals(8, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    @Test fun `win withHalf redouble scores 4 for declarer only`() {
        val (d, o1, o2) = totals(round(declarerWon = true, basePoints = 1, multiplier = Multiplier.REDOUBLE))
        assertEquals(4, d); assertEquals(0, o1); assertEquals(0, o2)
    }

    // ── Declarer loses ───────────────────────────────────────────────────────

    @Test fun `lose noTrick normal scores 3 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 3, multiplier = Multiplier.NORMAL))
        assertEquals(0, d); assertEquals(3, o1); assertEquals(3, o2)
    }

    @Test fun `lose withTrick normal scores 2 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 2, multiplier = Multiplier.NORMAL))
        assertEquals(0, d); assertEquals(2, o1); assertEquals(2, o2)
    }

    @Test fun `lose withHalf normal scores 1 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 1, multiplier = Multiplier.NORMAL))
        assertEquals(0, d); assertEquals(1, o1); assertEquals(1, o2)
    }

    @Test fun `lose noTrick double scores 6 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 3, multiplier = Multiplier.DOUBLE))
        assertEquals(0, d); assertEquals(6, o1); assertEquals(6, o2)
    }

    @Test fun `lose withTrick double scores 4 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE))
        assertEquals(0, d); assertEquals(4, o1); assertEquals(4, o2)
    }

    @Test fun `lose withHalf double scores 2 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 1, multiplier = Multiplier.DOUBLE))
        assertEquals(0, d); assertEquals(2, o1); assertEquals(2, o2)
    }

    @Test fun `lose noTrick redouble scores 12 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 3, multiplier = Multiplier.REDOUBLE))
        assertEquals(0, d); assertEquals(12, o1); assertEquals(12, o2)
    }

    @Test fun `lose withTrick redouble scores 8 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 2, multiplier = Multiplier.REDOUBLE))
        assertEquals(0, d); assertEquals(8, o1); assertEquals(8, o2)
    }

    @Test fun `lose withHalf redouble scores 4 for each opponent only`() {
        val (d, o1, o2) = totals(round(declarerWon = false, basePoints = 1, multiplier = Multiplier.REDOUBLE))
        assertEquals(0, d); assertEquals(4, o1); assertEquals(4, o2)
    }

    // ── Running total accumulation ───────────────────────────────────────────

    @Test fun `running totals sum correctly across multiple rounds`() {
        val (d, o1, o2) = totals(
            round(declarerWon = true,  basePoints = 3, multiplier = Multiplier.NORMAL),   // d+3
            round(declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE),   // o1+4, o2+4
            round(declarerWon = true,  basePoints = 1, multiplier = Multiplier.REDOUBLE)  // d+4
        )
        assertEquals(7, d); assertEquals(4, o1); assertEquals(4, o2)
    }
}
