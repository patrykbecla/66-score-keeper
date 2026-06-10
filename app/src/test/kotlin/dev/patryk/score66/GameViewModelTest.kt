package dev.patryk.score66

import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.PendingRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Phase 3 acceptance: bi-phasic interaction flow, multiplier toggle logic,
 * edge-case disabled-state table, and round-number correctness after undo+replay.
 */
class GameViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    // ── Phase 1 — Declarer + multiplier ──────────────────────────────────────

    @Test
    fun `select declarer creates pending with NORMAL multiplier and null won-lost`() {
        val vm = GameViewModel()
        vm.selectDeclarer(1)
        val pending = vm.state.value.pending!!
        assertEquals(1, pending.declarerId)
        assertEquals(Multiplier.NORMAL, pending.multiplier)
        assertNull(pending.declarerWon)
    }

    @Test
    fun `tapping active multiplier toggles back to NORMAL`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.DOUBLE)
        assertEquals(Multiplier.DOUBLE, vm.state.value.pending?.multiplier)
        vm.setMultiplier(Multiplier.DOUBLE)
        assertEquals(Multiplier.NORMAL, vm.state.value.pending?.multiplier)
    }

    @Test
    fun `tapping inactive multiplier switches DOUBLE to REDOUBLE`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.DOUBLE)
        vm.setMultiplier(Multiplier.REDOUBLE)
        assertEquals(Multiplier.REDOUBLE, vm.state.value.pending?.multiplier)
    }

    @Test
    fun `tapping REDOUBLE then DOUBLE switches correctly`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.REDOUBLE)
        vm.setMultiplier(Multiplier.DOUBLE)
        assertEquals(Multiplier.DOUBLE, vm.state.value.pending?.multiplier)
    }

    @Test
    fun `setMultiplier before selecting declarer is a no-op`() {
        val vm = GameViewModel()
        vm.setMultiplier(Multiplier.DOUBLE)
        assertNull(vm.state.value.pending)
    }

    // ── Phase 2 — Won/Lost + finalization ─────────────────────────────────────

    @Test
    fun `full declarer-wins flow finalizes hand and clears pending`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.DOUBLE)
        vm.setDeclarerWon(true)
        vm.finalizeHand(3)

        val state = vm.state.value
        assertNull(state.pending)
        assertEquals(1, state.rounds.size)
        assertEquals(6, state.totalFor(0))
        assertEquals(0, state.totalFor(1))
        assertEquals(0, state.totalFor(2))
    }

    @Test
    fun `full declarer-loses flow awards each opponent and clears pending`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.REDOUBLE)
        vm.setDeclarerWon(false)
        vm.finalizeHand(2)

        val state = vm.state.value
        assertNull(state.pending)
        assertEquals(0, state.totalFor(0))
        assertEquals(8, state.totalFor(1))
        assertEquals(8, state.totalFor(2))
    }

    @Test
    fun `selecting new declarer mid-hand fully resets pending`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0)
        vm.setMultiplier(Multiplier.DOUBLE)
        vm.setDeclarerWon(true)
        vm.selectDeclarer(2)   // change declarer before finalizing

        val pending = vm.state.value.pending!!
        assertEquals(2, pending.declarerId)
        assertEquals(Multiplier.NORMAL, pending.multiplier)
        assertNull(pending.declarerWon)
    }

    // ── Disabled-state edge-case table (Section 6) ───────────────────────────

    @Test
    fun `no declarer - won-lost disabled, outcome disabled`() {
        val state = AppState()
        val hasDeclarerSelected = state.pending != null
        val hasWonLost = state.pending?.declarerWon != null

        assertFalse("Won/Lost should be disabled", hasDeclarerSelected)
        assertFalse("Outcome should be disabled", hasWonLost)
    }

    @Test
    fun `declarer selected no won-lost - won-lost enabled, outcome disabled`() {
        val state = AppState(pending = PendingRound(declarerId = 0))
        val hasDeclarerSelected = state.pending != null
        val hasWonLost = state.pending?.declarerWon != null

        assertTrue("Won/Lost should be enabled", hasDeclarerSelected)
        assertFalse("Outcome should be disabled", hasWonLost)
    }

    @Test
    fun `declarer and won-lost selected - both enabled`() {
        val state = AppState(pending = PendingRound(declarerId = 0, declarerWon = false))
        val hasDeclarerSelected = state.pending != null
        val hasWonLost = state.pending?.declarerWon != null

        assertTrue("Won/Lost should be enabled", hasDeclarerSelected)
        assertTrue("Outcome should be enabled", hasWonLost)
    }

    // ── Round numbering ───────────────────────────────────────────────────────

    @Test
    fun `round numbers are sequential`() {
        val vm = GameViewModel()
        repeat(3) { i ->
            vm.selectDeclarer(i)
            vm.setDeclarerWon(true)
            vm.finalizeHand(1)
        }
        val numbers = vm.state.value.rounds.map { it.number }
        assertEquals(listOf(1, 2, 3), numbers)
    }

    @Test
    fun `round number reuses undone slot after replay`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        vm.selectDeclarer(1); vm.setDeclarerWon(false); vm.finalizeHand(2)
        vm.undo()
        vm.selectDeclarer(2); vm.setDeclarerWon(true); vm.finalizeHand(1)

        val rounds = vm.state.value.rounds
        assertEquals(2, rounds.size)
        assertEquals(1, rounds[0].number)
        assertEquals(2, rounds[1].number)
    }

    // ── Utility actions ───────────────────────────────────────────────────────

    @Test
    fun `undo on empty rounds list is a no-op`() {
        val vm = GameViewModel()
        vm.undo()
        assertEquals(0, vm.state.value.rounds.size)
        assertNull(vm.state.value.pending)
    }

    @Test
    fun `undo does not restore cleared pending`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        assertNull(vm.state.value.pending)
        vm.undo()
        assertNull(vm.state.value.pending)  // still null after undo
    }

    @Test
    fun `newGame clears rounds and pending but keeps names and language`() {
        val vm = GameViewModel()
        vm.updatePlayerName(0, "Alice")
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        // finalizeHand clears pending; start a fresh hand to make it non-null again
        vm.selectDeclarer(1)
        assertNotNull(vm.state.value.pending)

        vm.newGame()

        val state = vm.state.value
        assertEquals(0, state.rounds.size)
        assertNull(state.pending)
        assertEquals("Alice", state.players[0].name)
    }

    // ── Session history ───────────────────────────────────────────────────────

    @Test
    fun `newGame with rounds saves session to history`() {
        val vm = GameViewModel()
        vm.updatePlayerName(0, "Alice")
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)

        val roundsBefore = vm.state.value.rounds.toList()
        val playersBefore = vm.state.value.players.toList()
        vm.newGame()

        val state = vm.state.value
        assertEquals(1, state.history.size)
        assertEquals(roundsBefore, state.history[0].rounds)
        assertEquals(playersBefore, state.history[0].players)
        assertEquals(0, state.rounds.size)
        assertNull(state.pending)
    }

    @Test
    fun `newGame with empty rounds does not add session to history`() {
        val vm = GameViewModel()
        vm.newGame()
        assertEquals(0, vm.state.value.history.size)
    }

    @Test
    fun `multiple newGame calls accumulate sessions newest first`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        vm.newGame()
        vm.selectDeclarer(1); vm.setDeclarerWon(false); vm.finalizeHand(2)
        vm.newGame()

        val history = vm.state.value.history
        assertEquals(2, history.size)
        // Second newGame session should be prepended (savedAt >= first)
        assertTrue(history[0].savedAt >= history[1].savedAt)
    }

    @Test
    fun `deleteSession removes only the targeted session`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        vm.newGame()
        vm.selectDeclarer(1); vm.setDeclarerWon(false); vm.finalizeHand(2)
        vm.newGame()

        val history = vm.state.value.history
        assertEquals(2, history.size)
        val idToDelete = history[1].id
        vm.deleteSession(idToDelete)

        val remaining = vm.state.value.history
        assertEquals(1, remaining.size)
        assertEquals(history[0].id, remaining[0].id)
    }

    @Test
    fun `renameSession updates only the targeted session name`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        vm.newGame()

        val id = vm.state.value.history[0].id
        vm.renameSession(id, "Summer game")

        assertEquals("Summer game", vm.state.value.history[0].name)
    }

    @Test
    fun `renameSession with blank name keeps original name`() {
        val vm = GameViewModel()
        vm.selectDeclarer(0); vm.setDeclarerWon(true); vm.finalizeHand(3)
        vm.newGame()

        val id = vm.state.value.history[0].id
        val originalName = vm.state.value.history[0].name
        vm.renameSession(id, "   ")

        assertEquals(originalName, vm.state.value.history[0].name)
    }
}
