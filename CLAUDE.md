# 66 Score Counter — Android App

A minimal, battery-light Android app for tracking game-point scores in the card game **66 (Sechsundsechzig)** for 3 or 4 players. Native Kotlin + Jetpack Compose. No server, no network, no analytics, no background work.

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
- **Declarer wins** → declarer gains `basePoints × multiplier`; opponents gain 0; watcher gains 0.
- **Declarer loses** → **each active opponent** gains `basePoints × multiplier`; declarer and watcher gain 0.

**Multipliers:** Normal ×1 (3/2/1) · **Double (Kontra)** ×2 (6/4/2) · **Redouble (Re)** ×4 (12/8/4).
Both multiplier buttons are always tappable and mutually exclusive: tap an inactive one to switch, tap the active one to return to NORMAL.

---

## 3. Data model (`data/AppState.kt`)

```kotlin
@Serializable data class Player(val id: Int, val name: String)        // ids 0/1/2/3 stable

@Serializable enum class Multiplier(val factor: Int) { NORMAL(1), DOUBLE(2), REDOUBLE(4) }

@Serializable data class Round(
    val number: Int, val declarerId: Int, val declarerWon: Boolean,
    val basePoints: Int, val multiplier: Multiplier,
    val watcherId: Int? = null,   // 4P: sitting-out player; scores 0
    val numPlayers: Int? = null   // 4P: 4; null = legacy 3P round
) {
    val awarded get() = basePoints * multiplier.factor
    fun pointsFor(playerId: Int): Int    // 0 for watcher/out-of-range, else awarded to right side
}

@Serializable data class PendingRound(   // a hand in progress, null when none
    val declarerId: Int? = null,         // null until a declarer is tapped
    val watcherId: Int? = null,          // 4P: carries over from previous round
    val multiplier: Multiplier = Multiplier.NORMAL,
    val declarerWon: Boolean? = null     // null until Won/Lost tapped
)

@Serializable enum class Language { EN, PL }

@Serializable data class GameSession(    // an archived finished game
    val id: Long, val savedAt: Long, val name: String,
    val players: List<Player>, val rounds: List<Round>
)

@Serializable data class AppState(
    val players: List<Player> = /* Player 1/2/3/4 — always 4 entries */,
    val rounds: List<Round> = emptyList(),
    val pending: PendingRound? = null,
    val language: Language = Language.EN,
    val history: List<GameSession> = emptyList(),
    val fourPlayer: Boolean = false
) {
    val activePlayers: List<Player>      // players.take(4) or take(3) based on fourPlayer
    fun totalFor(playerId: Int): Int                 // sum of pointsFor across rounds
    fun cumulativeSeriesFor(playerId: Int): List<Int> // [0, running totals...] for the graph
}
```

`players` always holds 4 entries; old 3-player saves are padded to 4 on load in the ViewModel `init` block. `activePlayers` is the authoritative list for all UI iteration and archiving. Derived values (totals, cumulative series) are computed, never stored.

---

## 4. Main screen layout (`ui/ScoreKeeperApp.kt`)

**Top bar:** left = horizontally-scrollable row of **Undo / New game / Graph / History** text buttons; right = compact **3P/4P toggle** (3G/4G in Polish) + **language toggle** showing the *other* language ("PL" while in EN). Both right-side labels are plain `Text + Modifier.clickable` (no `TextButton`) to avoid Material's 58dp minimum-width constraint.

**Zone A — fixed controls:**
1. **Declarer selector row** — 3 or 4 equal cards (based on `activePlayers`): uppercased name + large bold total. Tap = select declarer; long-press = edit-name dialog. Selected card is highlighted. In 4P mode each card also shows a **⊘ icon** (bottom-right, 18sp) — tap to mark that player as the round's watcher; card dims to dark gray (`#131313`) and name/total turn gray. The watcher cannot be tapped as declarer. Watcher selection carries over to the next round automatically.
2. **Multiplier + Won/Lost row** — one row of four buttons: **Double · Re · Won · Lost**. Won/Lost disabled until a declarer is selected. Active buttons filled.
3. **Outcome row** — 3 buttons **No tricks · With trick · With half**, each rendered as `label • points` with the multiplier already applied (e.g. `No tricks • 6`). Disabled until Won/Lost is set. In 4P mode also requires a watcher to be selected.

**Zone B — scrollable history:** one block per active player (same order as selectors): uppercased name + a `FlowRow` of **score pills**, newest first. A pill appears only for hands that player scored. Each pill shows round number (top-left, gray), declarer initial (bottom-left, gray), and awarded points (right, large white). The multiplier is conveyed by a **red-tinted card background** (darker for Double, brighter for Redouble) — not a letter tag.

---

## 5. Interaction flow

**3P mode** — bi-phasic, declarer + multiplier set *before* the hand, outcome entered *after*:

1. Tap a player selector → sets `pending.declarerId`; Won/Lost enable.
2. (Optional) tap Double/Re → sets `pending.multiplier`.
3. Tap Won/Lost → sets `pending.declarerWon`; outcome buttons enable.
4. Tap an outcome → finalize: `number = rounds.size + 1`, append a `Round`, clear `pending`. Totals/pills update immediately.

**4P mode** — same flow with one extra step:

1. (Optional, sticky) Tap **⊘** on a player card → sets `pending.watcherId`; that player cannot be the declarer this round. Watcher is pre-filled from the previous round.
2–4. Same as 3P. Outcome buttons additionally require a watcher to be set.

**Undo** drops the most recent round (no-op if none; watcher carry-over in `pending` is unaffected).
**New game** → confirm dialog → archives `activePlayers` + rounds into `history` (named by timestamp) if rounds exist, then clears rounds and `pending`. Names, language, and player-mode are preserved.

---

## 6. History & graph

- **Graph** (`ui/GraphScreen.kt`): per-player totals row + a step-style cumulative line chart (Canvas) with axis labels and per-player colors (`theme/Color.kt playerColor`). Iterates `activePlayers`. Reachable from the main bar and from a session detail.
- **History list** (`ui/HistoryScreen.kt`): saved `GameSession`s, newest first, each with rename and delete (confirm dialog).
- **History detail**: read-only declarer row (watcher icons suppressed via `showWatcherIcons = false`) + per-player pills for that session, with a button to its own graph. Session state is reconstructed as `AppState(players, rounds, fourPlayer = players.size == 4)`.

---

## 7. Localization (`ui/Strings.kt`)

No Android string resources. A `Strings` data class with `EnStrings`/`PlStrings`, exposed via `LocalStrings` (a `CompositionLocal`) driven by `AppState.language`. The toggle flips EN⇄PL in the ViewModel and persists it — **no Activity recreation**. Add new UI text by adding a field to `Strings` and both instances. Player-mode toggle labels (`threePlayer`/`fourPlayer`) are localized: "3P"/"4P" in EN, "3G"/"4G" in PL.

---

## 8. Theme & performance

- Material 3 **dark** scheme always (`theme/Theme.kt`), near-black background for OLED battery; totals/names white and bold.
- Player line colors: green `#7A9945` · teal `#499FBA` · orange `#D18238` · purple `#BF60C9` (4th player).
- Inherently low-power: no background services, `WorkManager`, polling, networking, wake locks, or `FLAG_KEEP_SCREEN_ON`.
- Keep recompositions scoped — hoist state, immutable data classes, `key`s in lists. Persistence writes only happen on user taps (tiny JSON), so battery/IO is negligible.
- Backward-compatible serialization: `ignoreUnknownKeys = true`; new nullable fields (`watcherId`, `numPlayers`, `fourPlayer`) default to null/false when absent from old saves; old 3-player `players` lists are padded to 4 in the ViewModel `init` block.
