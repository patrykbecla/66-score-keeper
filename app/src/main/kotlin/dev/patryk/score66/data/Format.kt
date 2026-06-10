package dev.patryk.score66.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSessionTimestamp(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
