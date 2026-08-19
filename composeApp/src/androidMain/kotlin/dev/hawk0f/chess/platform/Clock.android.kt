package dev.hawk0f.chess.platform

import java.text.DateFormat
import java.util.Date

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
