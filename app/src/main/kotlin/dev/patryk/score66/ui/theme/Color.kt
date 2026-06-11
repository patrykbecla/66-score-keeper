package dev.patryk.score66.ui.theme

import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val NearBlack = Color(0xFF0D0D0D)
val Surface = Color(0xFF1A1A1A)
val OnSurface = Color(0xFFFFFFFF)
val Accent = Color(0xFF556B2F)
val AccentDim = Color(0xFF334018)
val Gray = Color(0xFF888888)
val Red = Color(0xFFE53935)

val PlayerLineColors = listOf(
    Color(0xFF7A9945), Color(0xFF499FBA), Color(0xFFD18238), Color(0xFFBF60C9)
)

fun playerColor(id: Int) = PlayerLineColors[id % PlayerLineColors.size]
