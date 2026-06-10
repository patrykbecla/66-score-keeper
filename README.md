# 66 Score Keeper

An Android score tracker for the card game 66 (Sechsundsechzig), built for 3 players.

## Features

- Track scores across rounds with Double (Kontra) and Redouble (Rekontra) multipliers
- Per-round history with score pills showing round number, declarer, and points awarded
- Score-over-time line graph per player
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
- Declarer loses → both opponents each gain `points × multiplier`

Multipliers: Normal ×1, Double ×2, Redouble ×4.

## Tech

Kotlin · Jetpack Compose · Material 3 · DataStore · kotlinx.serialization
