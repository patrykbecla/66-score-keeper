package dev.patryk.score66.ui

import androidx.compose.runtime.compositionLocalOf

data class Strings(
    val appTitle: String,
    val noTrick: String,
    val withTrick: String,
    val withHalf: String,
    val won: String,
    val lost: String,
    val double: String,
    val redouble: String,
    val undo: String,
    val newGame: String,
    val newGameConfirm: String,
    val editName: String,
    val doubleTag: String,
    val redoubleTag: String,
    val cancel: String,
    val ok: String,
    val graph: String,
    val home: String,
    val history: String,
    val rename: String,
    val delete: String,
    val deleteConfirm: String,
    val emptyHistory: String
)

val EnStrings = Strings(
    appTitle = "66 Counter",
    noTrick = "No tricks",
    withTrick = "With trick",
    withHalf = "With half",
    won = "Won",
    lost = "Lost",
    double = "Double",
    redouble = "Re",
    undo = "Undo",
    newGame = "New game",
    newGameConfirm = "Start a new game? Scores will be cleared.",
    editName = "Edit name",
    doubleTag = "D",
    redoubleTag = "R",
    cancel = "Cancel",
    ok = "OK",
    graph = "Graph",
    home = "Home",
    history = "History",
    rename = "Rename",
    delete = "Delete",
    deleteConfirm = "Delete this session?",
    emptyHistory = "No saved games yet"
)

val PlStrings = Strings(
    appTitle = "Licznik 66",
    noTrick = "Bez sztycha",
    withTrick = "Z sztychem",
    withHalf = "Z wodą",
    won = "Wygrana",
    lost = "Przegrana",
    double = "Kontra",
    redouble = "Re",
    undo = "Cofnij",
    newGame = "Nowa gra",
    newGameConfirm = "Rozpocząć nową grę? Wyniki zostaną wyczyszczone.",
    editName = "Edytuj imię",
    doubleTag = "K",
    redoubleTag = "R",
    cancel = "Anuluj",
    ok = "OK",
    graph = "Wykres",
    home = "Dom",
    history = "Historia",
    rename = "Zmień nazwę",
    delete = "Usuń",
    deleteConfirm = "Usunąć tę sesję?",
    emptyHistory = "Brak zapisanych gier"
)

val LocalStrings = compositionLocalOf { EnStrings }
