package dev.patryk.score66.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patryk.score66.data.AppState
import dev.patryk.score66.data.Language
import dev.patryk.score66.data.Multiplier
import dev.patryk.score66.data.PendingRound
import dev.patryk.score66.data.Player
import dev.patryk.score66.data.Round
import dev.patryk.score66.ui.theme.Gray
import dev.patryk.score66.ui.theme.Score66Theme
import dev.patryk.score66.ui.theme.playerColor
import kotlin.math.max

@Composable
fun GraphScreen(
    state: AppState,
    onNavigateHome: () -> Unit = {}
) {
    val strings = LocalStrings.current
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            PlayerTotalsRow(state = state)
            ScoreLineChart(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PlayerTotalsRow(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.players.forEach { player ->
            val color = playerColor(player.id)
            val total = state.totalFor(player.id)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = player.name.uppercase(),
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = total.toString(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp
                )
            }
        }
    }
}

@Composable
private fun ScoreLineChart(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val n = state.rounds.size
    if (n == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("—", color = Gray, fontSize = 24.sp)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    val allSeries = remember(state.rounds, state.players) {
        state.players.map { state.cumulativeSeriesFor(it.id) }
    }
    val maxY = remember(allSeries) { max(1, allSeries.flatten().max()) }

    val labelStyle = TextStyle(color = Gray, fontSize = 15.sp)
    val axisColor = Gray.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val leftPad = 42.dp.toPx()
        val rightPad = 16.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 40.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        // Axes
        drawLine(
            color = axisColor,
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(size.width - rightPad, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )

        // Y-axis: up to 10 evenly spaced integer labels from 0 to maxY
        val yStep = max(1, (maxY + 9) / 10)
        generateSequence(0) { it + yStep }.takeWhile { it <= maxY }.forEach { v ->
            val y = topPad + chartH - (v.toFloat() / maxY) * chartH
            val measured = textMeasurer.measure(v.toString(), labelStyle)
            drawText(
                textMeasurer,
                text = v.toString(),
                topLeft = Offset(leftPad - measured.size.width - 12.dp.toPx(), y - measured.size.height / 2f),
                style = labelStyle
            )
        }

        // X-axis: evenly spaced labels starting from 0
        val xStep = max(1, (n + 4) / 5)
        generateSequence(0) { it + xStep }.takeWhile { it <= n }.forEach { i ->
            val x = leftPad + i * (chartW / n)
            val measured = textMeasurer.measure(i.toString(), labelStyle)
            drawText(
                textMeasurer,
                text = i.toString(),
                topLeft = Offset(x - measured.size.width / 2f, topPad + chartH + 12.dp.toPx()),
                style = labelStyle
            )
        }

        // Lines for each player
        state.players.forEach { player ->
            val series = allSeries[player.id]
            val color = playerColor(player.id)
            val path = Path()
            var prevY = 0f
            series.forEachIndexed { i, value ->
                val x = leftPad + i * (chartW / n)
                val y = topPad + chartH - (value.toFloat() / maxY) * chartH
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, prevY)
                    path.lineTo(x, y)
                }
                prevY = y
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GraphScreenPreview() {
    val previewState = AppState(
        players = listOf(Player(0, "Alice"), Player(1, "Bob"), Player(2, "Carol")),
        rounds = listOf(
            Round(1, declarerId = 0, declarerWon = true,  basePoints = 3, multiplier = Multiplier.NORMAL),
            Round(2, declarerId = 1, declarerWon = false, basePoints = 2, multiplier = Multiplier.DOUBLE),
            Round(3, declarerId = 2, declarerWon = true,  basePoints = 1, multiplier = Multiplier.REDOUBLE),
            Round(4, declarerId = 0, declarerWon = false, basePoints = 3, multiplier = Multiplier.NORMAL),
        ),
        pending = PendingRound(declarerId = 1, multiplier = Multiplier.DOUBLE, declarerWon = false),
        language = Language.EN
    )
    Score66Theme {
        CompositionLocalProvider(LocalStrings provides EnStrings) {
            GraphScreen(state = previewState)
        }
    }
}
