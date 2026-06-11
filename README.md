# 66 Score Keeper

An Android score tracker for the card game 66 (Sechsundsechzig), supporting 3 or 4 players.

## Features

- **3-player and 4-player modes** — toggle between 3P/4P (3G/4G in Polish) at any time
- 4-player rotation: tap the ⊘ icon on a player card to mark who is sitting out; their score for that round is 0; watcher carries over between rounds automatically
- Track scores across rounds with Double (Kontra) and Redouble (Rekontra) multipliers
- Per-round history with score pills showing round number, declarer, and points awarded
- Score-over-time line graph per player (4 lines in 4-player mode)
- Game history — finished games are auto-archived; browse, rename, or delete past sessions
- Editable player names (long-press to edit)
- Undo last round
- EN / PL language toggle
- All state persists across app restarts

## Scoring

Each round has a declarer. Outcomes are described from the loser's perspective:

| Outcome | Base points |
|---|:---:|
| No tricks (Schwarz) | 3 |
| With trick (Schneider) | 2 |
| With half (33+) | 1 |

- Declarer wins → declarer gains `points × multiplier`
- Declarer loses → each active opponent gains `points × multiplier` (sitting-out player scores 0)

Multipliers: Normal ×1, Double ×2, Redouble ×4.

## Tech

Kotlin · Jetpack Compose · Material 3 · DataStore · kotlinx.serialization
