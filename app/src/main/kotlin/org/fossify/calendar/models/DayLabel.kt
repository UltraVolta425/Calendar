package org.fossify.calendar.models

/**
 * A line of text shown below the date in the daily view.
 * It counts the years, months and days elapsed since [startDayCode] (yyyyMMdd).
 */
data class DayLabel(val title: String, val startDayCode: String)
