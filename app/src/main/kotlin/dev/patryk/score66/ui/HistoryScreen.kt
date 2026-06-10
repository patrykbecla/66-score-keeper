package dev.patryk.score66.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.GameSession
import dev.patryk.score66.data.formatSessionTimestamp
import dev.patryk.score66.ui.theme.Gray

@Composable
fun HistoryListScreen(
    sessions: List<GameSession>,
    onOpenSession: (Long) -> Unit = {},
    onRename: (Long, String) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onNavigateHome: () -> Unit = {}
) {
    val strings = LocalStrings.current
    var renamingSessionId by remember { mutableStateOf<Long?>(null) }
    var deletingSessionId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateHome) {
                    Text(strings.home, color = Gray)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.emptyHistory, color = Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sessions.sortedByDescending { it.savedAt },
                    key = { it.id }
                ) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenSession(session.id) },
                        onRename = { renamingSessionId = session.id },
                        onDelete = { deletingSessionId = session.id }
                    )
                }
            }
        }
    }

    renamingSessionId?.let { id ->
        val session = sessions.firstOrNull { it.id == id } ?: return@let
        EditNameDialog(
            currentName = session.name,
            strings = strings,
            title = strings.rename,
            onConfirm = { newName ->
                onRename(id, newName)
                renamingSessionId = null
            },
            onDismiss = { renamingSessionId = null }
        )
    }

    deletingSessionId?.let { id ->
        DeleteSessionConfirmDialog(
            strings = strings,
            onConfirm = {
                onDelete(id)
                deletingSessionId = null
            },
            onDismiss = { deletingSessionId = null }
        )
    }
}

@Composable
private fun SessionRow(
    session: GameSession,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatSessionTimestamp(session.savedAt),
                    color = Gray,
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = onRename) { Text(strings.rename, color = Gray, fontSize = 13.sp) }
            TextButton(onClick = onDelete) { Text(strings.delete, color = Gray, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun DeleteSessionConfirmDialog(
    strings: Strings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                strings.deleteConfirm,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.ok) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun HistoryDetailScreen(
    session: GameSession,
    onNavigateBack: () -> Unit = {},
    onOpenGraph: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val state = remember(session) {
        AppState(players = session.players, rounds = session.rounds)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onNavigateBack) { Text(strings.home, color = Gray) }
                TextButton(onClick = onOpenGraph) { Text(strings.graph, color = Gray) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                DeclarerSelectorRow(state = state)
                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.background)

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
    }
}
