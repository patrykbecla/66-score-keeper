package dev.patryk.score66.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.patryk.score66.GameViewModel
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.DataStoreStorage
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Player
import dev.patryk.score66.data.Round
import dev.patryk.score66.data.dataStore
import dev.patryk.score66.ui.theme.Gray
import dev.patryk.score66.ui.theme.Red
import dev.patryk.score66.ui.theme.Score66Theme

// ── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun ScoreKeeperApp() {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(DataStoreStorage(context.applicationContext.dataStore)) as T
                }
            }
        }
    )
    val state by vm.state.collectAsState()
    val strings = if (state.language == Language.EN) EnStrings else PlStrings
    CompositionLocalProvider(LocalStrings provides strings) {
        ScoreKeeperScreen(
            state = state,
            onSelectDeclarer = vm::selectDeclarer,
            onSetMultiplier = vm::setMultiplier,
            onSetDeclarerWon = vm::setDeclarerWon,
            onFinalizeHand = vm::finalizeHand,
            onUndo = vm::undo,
            onNewGame = vm::newGame,
            onToggleLanguage = vm::toggleLanguage,
            onEditPlayerName = vm::updatePlayerName
        )
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreKeeperScreen(
    state: AppState,
    onSelectDeclarer: (Int) -> Unit = {},
    onSetMultiplier: (Multiplier) -> Unit = {},
    onSetDeclarerWon: (Boolean) -> Unit = {},
    onFinalizeHand: (Int) -> Unit = {},
    onUndo: () -> Unit = {},
    onNewGame: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onEditPlayerName: (Int, String) -> Unit = { _, _ -> }
) {
    val strings = LocalStrings.current
    val multiplier = state.pending?.multiplier ?: Multiplier.NORMAL
    val hasDeclarerSelected = state.pending != null
    val hasWonLost = state.pending?.declarerWon != null

    var editingPlayerId by remember { mutableStateOf<Int?>(null) }
    var showNewGameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                actions = {
                    TextButton(onClick = onToggleLanguage) {
                        Text(
                            text = if (state.language == Language.EN) "PL" else "EN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Zone A — fixed controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                DeclarerSelectorRow(
                    state = state,
                    onSelect = onSelectDeclarer,
                    onEditName = { id, _ -> editingPlayerId = id }
                )
                MultiplierRow(
                    activeMultiplier = multiplier,
                    onSelect = onSetMultiplier
                )
                WonLostRow(
                    declarerWon = state.pending?.declarerWon,
                    enabled = hasDeclarerSelected,
                    onSelect = onSetDeclarerWon
                )
                OutcomeRow(
                    multiplier = multiplier,
                    enabled = hasWonLost,
                    onFinalize = onFinalizeHand
                )
                UtilityRow(onUndo = onUndo, onNewGame = { showNewGameDialog = true })
                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(color = Gray.copy(alpha = 0.25f))

            // Zone B — scrollable history
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.players.forEach { player ->
                    key(player.id) {
                        PlayerHistoryBlock(player = player, state = state)
                    }
                }
            }
        }

        // ── Dialogs ──────────────────────────────────────────────────────────
        editingPlayerId?.let { id ->
            val player = state.players.firstOrNull { it.id == id } ?: return@let
            EditNameDialog(
                currentName = player.name,
                strings = strings,
                onConfirm = { newName ->
                    onEditPlayerName(id, newName)
                    editingPlayerId = null
                },
                onDismiss = { editingPlayerId = null }
            )
        }

        if (showNewGameDialog) {
            NewGameConfirmDialog(
                strings = strings,
                onConfirm = {
                    onNewGame()
                    showNewGameDialog = false
                },
                onDismiss = { showNewGameDialog = false }
            )
        }
    }
}

// ── Zone A composables ────────────────────────────────────────────────────────

@Composable
fun DeclarerSelectorRow(
    state: AppState,
    onSelect: (Int) -> Unit = {},
    onEditName: (Int, String) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.players.forEach { player ->
            key(player.id) {
                val isSelected = state.pending?.declarerId == player.id
                val total = state.totalFor(player.id)
                PlayerSelectorCard(
                    name = player.name,
                    total = total,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(player.id) },
                    onLongClick = { onEditName(player.id, player.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSelectorCard(
    name: String,
    total: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Gray.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = total.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp
            )
        }
    }
}

@Composable
fun MultiplierRow(activeMultiplier: Multiplier, onSelect: (Multiplier) -> Unit = {}) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameToggleButton(
            label = strings.double,
            active = activeMultiplier == Multiplier.DOUBLE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(Multiplier.DOUBLE) }
        )
        GameToggleButton(
            label = strings.redouble,
            active = activeMultiplier == Multiplier.REDOUBLE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(Multiplier.REDOUBLE) }
        )
    }
}

@Composable
fun WonLostRow(
    declarerWon: Boolean?,
    enabled: Boolean,
    onSelect: (Boolean) -> Unit = {}
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameToggleButton(
            label = strings.won,
            active = declarerWon == true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(true) }
        )
        GameToggleButton(
            label = strings.lost,
            active = declarerWon == false,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(false) }
        )
    }
}

@Composable
fun OutcomeRow(
    multiplier: Multiplier,
    enabled: Boolean,
    onFinalize: (Int) -> Unit = {}
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutcomeButton(
            label = "${strings.noTrick}\n(${3 * multiplier.factor})",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onFinalize(3) }
        )
        OutcomeButton(
            label = "${strings.withTrick}\n(${2 * multiplier.factor})",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onFinalize(2) }
        )
        OutcomeButton(
            label = "${strings.withHalf}\n(${1 * multiplier.factor})",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onFinalize(1) }
        )
    }
}

@Composable
fun UtilityRow(onUndo: () -> Unit = {}, onNewGame: () -> Unit = {}) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onUndo) { Text(strings.undo, color = Gray) }
        TextButton(onClick = onNewGame) { Text(strings.newGame, color = Gray) }
    }
}

// ── Reusable button components ────────────────────────────────────────────────

@Composable
fun GameToggleButton(
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Gray.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            when {
                !enabled -> Gray.copy(alpha = 0.2f)
                active -> MaterialTheme.colorScheme.primary
                else -> Gray.copy(alpha = 0.55f)
            }
        )
    ) {
        Text(label)
    }
}

@Composable
fun OutcomeButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Gray.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, if (enabled) Gray.copy(alpha = 0.55f) else Gray.copy(alpha = 0.2f))
    ) {
        Text(label, textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 15.sp)
    }
}

// ── Zone B composables ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerHistoryBlock(player: Player, state: AppState) {
    val strings = LocalStrings.current
    val scoredRounds = state.rounds.filter { it.pointsFor(player.id) > 0 }.reversed()

    Column {
        Text(
            text = player.name,
            color = Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            scoredRounds.forEach { round ->
                key(round.number) {
                    val declarerName = state.players
                        .firstOrNull { it.id == round.declarerId }?.name.orEmpty()
                    val tag = when (round.multiplier) {
                        Multiplier.DOUBLE -> strings.doubleTag
                        Multiplier.REDOUBLE -> strings.redoubleTag
                        Multiplier.NORMAL -> ""
                    }
                    ScorePill(
                        roundNumber = round.number,
                        declarerInitial = declarerName.firstOrNull()
                            ?.uppercaseChar()?.toString().orEmpty(),
                        score = round.awarded,
                        tag = tag
                    )
                }
            }
        }
    }
}

@Composable
fun ScorePill(roundNumber: Int, declarerInitial: String, score: Int, tag: String) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .width(64.dp)
                .height(52.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            // Left: round number (top) + declarer initial (bottom)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(roundNumber.toString(), color = Gray, fontSize = 10.sp, lineHeight = 12.sp)
                Text(declarerInitial, color = Gray, fontSize = 10.sp, lineHeight = 12.sp)
            }
            // Right: score (top) + multiplier tag (bottom)
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = score.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 24.sp
                )
                Box(modifier = Modifier.height(12.dp), contentAlignment = Alignment.BottomEnd) {
                    if (tag.isNotEmpty()) {
                        Text(tag, color = Red, fontSize = 10.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun EditNameDialog(
    currentName: String,
    strings: Strings,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editName, color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim().ifEmpty { currentName }) }) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun NewGameConfirmDialog(
    strings: Strings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(strings.newGameConfirm) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(strings.ok) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScoreKeeperScreenPreview() {
    // Seeded data to verify Phase 2 acceptance check:
    // Alice: R1(win, 3×N=3) + R2(opp of Bob, 2×D=4) → total 7; pills newest-first: [R2, R1]
    // Bob:   R4(opp of Alice, 3×N=3) → total 3; pills: [R4]
    // Carol: R2(opp of Bob, 2×D=4) + R3(win, 1×R=4) + R4(opp of Alice, 3×N=3) → total 11; pills newest-first: [R4, R3, R2]
    val previewState = AppState(
        players = listOf(Player(0, "Alice"), Player(1, "Bob"), Player(2, "Carol")),
        rounds = listOf(
            Round(1, declarerId = 0, declarerWon = true,  basePoints = 3, multiplier = Multiplier.NORMAL),
            Round(2, declarerId = 1, declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE),
            Round(3, declarerId = 2, declarerWon = true,  basePoints = 1, multiplier = Multiplier.REDOUBLE),
            Round(4, declarerId = 0, declarerWon = false, basePoints = 3, multiplier = Multiplier.NORMAL),
        ),
        pending = PendingRound(declarerId = 1, multiplier = Multiplier.DOUBLE, declarerWon = false)
    )
    Score66Theme {
        CompositionLocalProvider(LocalStrings provides EnStrings) {
            ScoreKeeperScreen(state = previewState)
        }
    }
}
