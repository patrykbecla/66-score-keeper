# 66 Score Counter — Android App Handoff

A minimal, battery-light Android app for tracking game-point scores in the card game **66 (Sechsundsechzig)** for 3 players. Built native in Kotlin + Jetpack Compose. No server, no network, no analytics, no background work.

The App is now fully built and functional. All future changes may deviate from the details listed below.

---

## 1. Scope & non-goals

**In scope:** local score tracking, dark UI, editable player names, per-round scoring with double/redouble, EN/PL toggle, undo, new-game reset, persistence across process death.

**Explicitly out of scope (do NOT build):** accounts, cloud sync, networking, statistics/history across games, animations beyond trivial state changes, settings screens, ads, sound, landscape-specific layouts (portrait only is fine).

---

## 2. Tech stack & project config

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** single `Activity`, single `ViewModel`, unidirectional state. No fragments, no navigation library (one screen).
- **Persistence:** Preferences **DataStore** storing ONE serialized JSON string. Use `kotlinx.serialization`. Do **not** use Room.
- **minSdk:** 24 · **targetSdk:** latest stable · **compileSdk:** latest stable
- **Orientation:** portrait only (`android:screenOrientation="portrait"`).
- **No** other dependencies unless strictly required by the above.

---

## 3. Game rules the scoring must implement

Each hand has a **declarer** (the player who picked trump). The outcome buttons describe **the loser's situation** — which is the declarer if they lost, or the opponents if the declarer won. Points are assigned automatically based on whether the declarer won or lost.

| Outcome button (EN) | Loser's situation                     | Base points |
|---------------------|---------------------------------------|:-----------:|
| No tricks           | Loser took **no tricks** (Schwarz)    | **3**       |
| With trick          | Loser took a trick, stayed under 33 (Schneider) | **2** |
| With half (33+)     | Loser reached **33+** card points     | **1**       |

**Point assignment:**
- **Declarer wins** → declarer gains `basePoints × multiplier`; both opponents gain 0.
- **Declarer loses** → **both opponents each** gain `basePoints × multiplier`; declarer gains 0.

**Multipliers (per hand):**

- Normal: ×1 → awards **3 / 2 / 1**
- **Double (Kontra):** ×2 → awards **6 / 4 / 2**
- **Redouble (Rekontra):** ×4 → awards **12 / 8 / 4**

**Multiplier toggles (non-sequential, always tappable — confirmed by owner):**
- Both Double and Redouble buttons are **always enabled**.
- Tapping an inactive button activates it and deactivates the other (mutually exclusive).
- Tapping the currently active button deactivates it, returning to `NORMAL`.

---

## 4. Data model

```kotlin
@Serializable
data class Player(
    val id: Int,            // 0, 1, 2 — stable
    val name: String        // editable, defaults "Player 1/2/3"
)

@Serializable
enum class Multiplier(val factor: Int) {
    NORMAL(1), DOUBLE(2), REDOUBLE(4)
    // Display tags ("D"/"R" in EN, "K"/"R" in PL) come from the Strings layer, not hardcoded here
}

@Serializable
data class Round(
    val number: Int,            // sequential hand number, assigned at finalization (see Section 6)
    val declarerId: Int,        // which player declared trump
    val declarerWon: Boolean,   // true = declarer scored; false = both opponents scored
    val basePoints: Int,        // 3, 2, or 1 (from the outcome button)
    val multiplier: Multiplier
) {
    val awarded: Int get() = basePoints * multiplier.factor
}

// Tracks a hand that has been started (declarer + multiplier set) but not yet scored
@Serializable
data class PendingRound(
    val declarerId: Int,
    val multiplier: Multiplier = Multiplier.NORMAL,
    val declarerWon: Boolean? = null   // null until Won/Lost is tapped
)

@Serializable
enum class Language { EN, PL }

@Serializable
data class AppState(
    val players: List<Player> = listOf(
        Player(0, "Player 1"), Player(1, "Player 2"), Player(2, "Player 3")
    ),
    val rounds: List<Round> = emptyList(),
    val pending: PendingRound? = null,  // non-null while a hand is in progress
    val language: Language = Language.EN
)
```

**Derived values (compute, never store):**
- Points earned by player `p` in round `r`:
  - If `r.declarerWon && p.id == r.declarerId` → `r.awarded`
  - If `!r.declarerWon && p.id != r.declarerId` → `r.awarded`
  - Otherwise → 0
- Running total for player `p` = sum of the above across all rounds.
- Declarer initial for round `r` = first character of the name of the player whose `id == r.declarerId`, uppercased.
- Multiplier chip tag = `strings.doubleTag` if `DOUBLE`, `strings.redoubleTag` if `REDOUBLE`, empty if `NORMAL`.

---

## 5. Screen layout (single screen, portrait)

The screen is split into two vertical zones:

### Zone A — Controls (fixed, never scrolls, top of screen)

1. **App bar:** title (small) on the left; **language toggle** (shows the *other* language, e.g. "PL" when in EN) on the right.
2. **Declarer selector row** — three equal tap targets side by side, each showing:
   - Editable **name** (medium, white). Tap-and-hold (or a dedicated small edit icon) to open a small edit dialog. Regular tap = select as declarer.
   - **Running total** (large, white, bold) directly under the name.
   - Visibly highlighted when that player is the selected declarer.
3. **Multiplier row** — two buttons: **Double** and **Redouble**. Both always enabled. Active state clearly indicated. Tapping the active button deactivates it; tapping the other switches to it.
4. **Won/Lost row** — two buttons: **Won** and **Lost**. Disabled until a declarer is selected. Visibly toggled when one is active.
5. **Outcome button row** — three buttons, left→right: **No tricks / With trick / With half (33+)**. Each shows its current effective point value in parentheses (e.g. "No tricks (6)" when Double is active). Disabled until Won/Lost is selected.
6. **Utility row** — **Undo** on the left, **New game** on the right.

> **UI state summary:**
> - No declarer selected → Won/Lost disabled, outcome buttons disabled.
> - Declarer selected, no Won/Lost → outcome buttons disabled.
> - Declarer + Won/Lost selected → outcome buttons enabled. Tapping one completes the hand.

### Zone B — Hand history (scrollable, below controls)

Three stacked player blocks, one per player, in the same left-to-right order as the selector row above. Each block contains:

- **Player label** (small, dimmed) — name only, for reference while scrolling.
- **Score sub-row** — a wrapping flow of score pills, newest prepended on the left. **Pills only appear when that player scored that hand** — no blank/`–` placeholders. Each pill is a small card with a 2×2 layout:

```
┌─────────────────┐
│ #N          [score] │
│ [initial]     [tag] │
└─────────────────┘
```

  - **Top-left:** round number (small, gray) e.g. `3`
  - **Bottom-left:** declarer's initial (small, gray) e.g. `E`
  - **Right / main:** awarded points (large, white, right-aligned) e.g. `2`
  - **Bottom-right:** multiplier tag (small, **red**, right-aligned) — `D`/`K` or `R` only if that hand was doubled/redoubled; absent otherwise

  Example pill — round 3, declarer Ethan, 2 points, Doubled: top-left `3`, bottom-left `E`, right `2`, bottom-right red `D`.

A vertical scrollbar appears on the right edge of Zone B when content exceeds the visible area (roughly 4 chip-rows deep). No hand-count limit.

---

## 6. Interaction flow (the core loop)

The flow is **bi-phasic**: the declarer and multiplier are set *before* the hand is played; the outcome is entered *after*.

### Phase 1 — Before the hand

1. Tap a **player selector** → sets `pending.declarerId`; that selector highlights. Won/Lost buttons enable.
2. (Optional) Tap **Double** or **Redouble** → sets `pending.multiplier`; tapping the active button returns to `NORMAL`; tapping the other switches to it.

At this point the hand is played.

### Phase 2 — After the hand

3. Tap **Won** or **Lost** → sets `pending.declarerWon`; that button highlights. Outcome buttons enable.
4. Tap one **outcome button** (No tricks / With trick / With half) → completes the hand:
   - `round.number` is assigned as `currentRounds.size + 1` at the moment of finalization (so after an undo + replay, the slot number is correctly reused).
   - A `Round` is finalized from `pending` + `basePoints` (3/2/1) and prepended to Zone B.
   - Points are assigned per Section 3: declarer gets `awarded` if they won; both opponents each get `awarded` if the declarer lost.
   - **Reset:** clear `pending` entirely (declarer, multiplier, Won/Lost all reset). Running totals and score chips update immediately.

### Utility actions

- **Undo** — removes the most recently prepended round; running totals update. No-op if no rounds exist. Does **not** restore a cleared `pending` state.
- **New game** — confirm dialog → clears all rounds and `pending`; names and language preserved.

### Edge-case summary

| State | Won/Lost buttons | Outcome buttons |
|---|---|---|
| No declarer selected | Disabled | Disabled |
| Declarer selected, no Won/Lost | Enabled | Disabled |
| Declarer + Won/Lost selected | Enabled | **Enabled** |

---

## 7. Theme

- Material 3 **dark** color scheme, applied always (do not follow system light/dark — always dark).
- Background near-black (helps OLED battery). Player totals and names in **white**; round numbers white but smaller. Buttons with a muted accent.
- No custom fonts required; system default is fine. Bold weight for totals.

---

## 8. Localization

Small fixed string set — do **not** use Android string resources/locale switching. Instead:

- Define a `Strings` interface (or data class) with one field per UI string; provide `EnStrings` and `PlStrings`.
- Expose via a Compose `CompositionLocal` (e.g. `LocalStrings`), driven by `AppState.language`.
- Language toggle flips `EN ⇄ PL` in the ViewModel and persists it. **No Activity recreation.**

Strings to translate (provide both):

| key            | EN                  | PL                                                               |
|----------------|---------------------|------------------------------------------------------------------|
| appTitle       | 66 Counter          | Licznik 66                                                       |
| noTrick        | No tricks           | Bez sztycha                                                      |
| withTrick      | With trick          | Z sztychem                                                       |
| withHalf       | With half (33+)     | Z wodą                                                           |
| won            | Won                 | Wygrana                                                          |
| lost           | Lost                | Przegrana                                                        |
| double         | Double              | Kontra                                                           |
| redouble       | Redouble            | Rekontra                                                         |
| undo           | Undo                | Cofnij                                                           |
| newGame        | New game            | Nowa gra                                                         |
| newGameConfirm | Start a new game? Scores will be cleared. | Rozpocząć nową grę? Wyniki zostaną wyczyszczone. |
| editName       | Edit name           | Edytuj imię                                                      |
| doubleTag      | D                   | K                                                                |
| redoubleTag    | R                   | R                                                                |
| cancel         | Cancel              | Anuluj                                                           |
| ok             | OK                  | OK                                                               |

---

## 9. Persistence

- Hold `AppState` as `StateFlow` in the ViewModel.
- On **every** mutation, serialize `AppState` to JSON and write the single string to Preferences DataStore. Writes are tiny and only happen on user taps — negligible battery/IO.
- On launch, read and deserialize; fall back to default `AppState` if absent or corrupt.

---

## 10. Battery / performance notes

This app is inherently low-power; do not over-engineer. Just avoid the few real pitfalls:

- No background services, `WorkManager`, polling, networking, or wake locks.
- Do **not** keep the screen on (no `FLAG_KEEP_SCREEN_ON`).
- Keep Compose recompositions scoped: hoist state, use stable/immutable data classes, avoid recomposing the whole tree on each tap. Use `key`s in the round list.
- Always-dark theme reduces OLED draw.