package com.trove.app.util

import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** Groups used by the Timeline view; order matches display order. */
enum class TimelineSection(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("Earlier This Month"),
    OLDER("Older"),
}

fun timelineSectionFor(epochSec: Long, now: LocalDate = LocalDate.now()): TimelineSection {
    val date = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        date.isEqual(now) -> TimelineSection.TODAY
        date.isEqual(now.minusDays(1)) -> TimelineSection.YESTERDAY
        date.isAfter(now.minusDays(7)) -> TimelineSection.THIS_WEEK
        date.month == now.month && date.year == now.year -> TimelineSection.THIS_MONTH
        else -> TimelineSection.OLDER
    }
}

fun monthYearLabel(epochSec: Long): String {
    val date = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
}

fun formatDateTime(epochSec: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(epochSec * 1000))

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}
