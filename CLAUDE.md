# 66 Score Counter — Android App

A minimal, battery-light Android app for tracking game-point scores in the card game **66 (Sechsundsechzig)** for 3 players. Native Kotlin + Jetpack Compose. No server, no network, no analytics, no background work.

The app is fully built and functional. This doc describes the current state; future changes may deviate.

---

## 0. Deploying to device

The system Java (1.8) is too old for the Gradle/AGP in use — use Java 17 from Homebrew, run from the repo root:

```bash
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home ./gradlew installDebug
```

Installs directly to any connected ADB device. `./gradlew test` runs the unit tests.

---

## 1. Tech stack & architecture

- **Kotlin + Jetpack Compose (Material 3)**, single `Activity`, single `GameViewModel`, unidirectional state.
- **In-app navigation** is a `Route` enum (`MAIN`, `GRAPH`, `HISTORY_LIST`, `HISTORY_DETAIL`, `HISTORY_GRAPH`) switched in `ScoreKeeperApp`, with a `BackHandler`. No navigation library, no fragments.
- **Persistence:** Preferences **DataStore** holding ONE serialized JSON string (`kotlinx.serialization`). Abstracted behind the `StateStorage` interface — `DataStoreStorage` in the app, `NoOpStorage` for tests/previews. No Room.
- **minSdk 24**, targetSdk/compileSdk latest stable. **Portrait only**, always-dark theme.
- Custom variable font **Exo2** (`theme/Fonts.kt`) for names/labels. No other non-essential dependencies.

---

## 2. Game rules (scoring)

Each hand has a **declarer** (picked trump). The outcome buttons describe **the loser's situation** (the declarer if they lost, the opponents if the declarer won). Points are assigned automatically from declarer-won/lost.

| Outcome button (EN) | Loser's situation                               | Base points |
|---------------------|-------------------------------------------------|:-----------:|
| No tricks           | Loser took **no tricks** (Schwarz)              | **3**       |
| With trick          | Loser took a trick, stayed under 33 (Schneider) | **2**       |
| With half           | Loser reached **33+** card points               | **1**       |

**Point assignment** (`Round.pointsFor`):
- **Declarer wins** → declarer gains `basePoints × multiplier`; opponents gain 0.
- **Declarer loses** → **each opponent** gains `basePoints × multiplier`; declarer gains 0.

**Multipliers:** Normal ×1 (3/2/1) · **Double (Kontra)** ×2 (6/4/2) · **Redouble (Re)** ×4 (12/8/4).
Both multiplier buttons are always tappable and mutually exclusive: tap an inactive one to switch, tap the active one to return to NORMAL.

---

## 3. Data model (`data/AppState.kt`)

```kotlin
@Serializable data class Player(val id: Int, val name: String)        // ids 0/1/2 stable

@Serializable enum class Multiplier(val factor: Int) { NORMAL(1), DOUBLE(2), REDOUBLE(4) }

@Serializable data class Round(
    val number: Int, val declarerId: Int, val declarerWon: Boolean,
    val basePoints: Int, val multiplier: Multiplier
) {
    val awarded get() = basePoints * multiplier.factor
    fun pointsFor(playerId: Int): Int    // awarded to the right side, else 0
}

@Serializable data class PendingRound(   // a hand in progress, null when none
    val declarerId: Int,
    val multiplier: Multiplier = Multiplier.NORMAL,
    val declarerWon: Boolean? = null     // null until Won/Lost tapped
)

@Serializable enum class Language { EN, PL }

@Serializable data class GameSession(    // an archived finished game
    val id: Long, val savedAt: Long, val name: String,
    val players: List<Player>, val rounds: List<Round>
)

@Serializable data class AppState(
    val players: List<Player> = /* Player 1/2/3 */,
    val rounds: List<Round> = emptyList(),
    val pending: PendingRound? = null,
    val language: Language = Language.EN,
    val history: List<GameSession> = emptyList()
) {
    fun totalFor(playerId: Int): Int                 // sum of pointsFor across rounds
    fun cumulativeSeriesFor(playerId: Int): List<Int> // [0, running totals...] for the graph
}
```

Derived values (totals, declarer initial, cumulative series) are computed, never stored.

---

## 4. Main screen layout (`ui/ScoreKeeperApp.kt`)

**Top bar:** left = horizontally-scrollable row of **Undo / New game / Graph / History** text buttons; right = **language toggle** showing the *other* language ("PL" while in EN).

**Zone A — fixed controls:**
1. **Declarer selector row** — 3 equal cards: uppercased name + large bold total. Tap = select declarer; long-press = edit-name dialog. Selected card is highlighted.
2. **Multiplier + Won/Lost row** — one row of four buttons: **Double · Re · Won · Lost**. Won/Lost disabled until a declarer is selected. Active buttons filled.
3. **Outcome row** — 3 buttons **No tricks · With trick · With half**, each rendered as `label • points` with the multiplier already applied (e.g. `No tricks • 6`). Disabled until Won/Lost is set.

**Zone B — scrollable history:** one block per player (same order as selectors): uppercased name + a `FlowRow` of **score pills**, newest first. A pill appears only for hands that player scored. Each pill shows round number (top-left, gray), declarer initial (bottom-left, gray), and awarded points (right, large white). The multiplier is conveyed by a **red-tinted card background** (darker for Double, brighter for Redouble) — not a letter tag.

---

## 5. Interaction flow

Bi-phasic — declarer + multiplier set *before* the hand, outcome entered *after*:

1. Tap a player selector → sets `pending.declarerId`; Won/Lost enable.
2. (Optional) tap Double/Re → sets `pending.multiplier`.
3. Tap Won/Lost → sets `pending.declarerWon`; outcome buttons enable.
4. Tap an outcome → finalize: `number = rounds.size + 1`, append a `Round`, clear `pending`. Totals/pills update immediately.

**Undo** drops the most recent round (no-op if none; does not restore `pending`).
**New game** → confirm dialog → archives the current game into `history` (named by timestamp) if it has rounds, then clears rounds and `pending`. Names and language are preserved.

---

## 6. History & graph

- **Graph** (`ui/GraphScreen.kt`): per-player totals row + a step-style cumulative line chart (Canvas) with axis labels and per-player colors (`theme/Color.kt playerColor`). Reachable from the main bar and from a session detail.
- **History list** (`ui/HistoryScreen.kt`): saved `GameSession`s, newest first, each with rename and delete (confirm dialog).
- **History detail**: read-only declarer row + per-player pills for that session, with a button to its own graph.

---

## 7. Localization (`ui/Strings.kt`)

No Android string resources. A `Strings` data class with `EnStrings`/`PlStrings`, exposed via `LocalStrings` (a `CompositionLocal`) driven by `AppState.language`. The toggle flips EN⇄PL in the ViewModel and persists it — **no Activity recreation**. Add new UI text by adding a field to `Strings` and both instances.

---

## 8. Theme & performance

- Material 3 **dark** scheme always (`theme/Theme.kt`), near-black background for OLED battery; totals/names white and bold.
- Inherently low-power: no background services, `WorkManager`, polling, networking, wake locks, or `FLAG_KEEP_SCREEN_ON`.
- Keep recompositions scoped — hoist state, immutable data classes, `key`s in lists. Persistence writes only happen on user taps (tiny JSON), so battery/IO is negligible.
