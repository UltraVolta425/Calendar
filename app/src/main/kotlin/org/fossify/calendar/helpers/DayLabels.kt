package org.fossify.calendar.helpers

import android.content.res.Resources
import org.fossify.calendar.R
import org.fossify.calendar.models.DayLabel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.format.ResolverStyle

object DayLabels {
    private const val KEY_TITLE = "title"
    private const val KEY_START = "start"
    private val DAY_CODE_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)

    /** Years, months and days from [start] to [day], or null if [day] is before [start]. */
    fun elapsed(start: LocalDate, day: LocalDate): Period? {
        return if (day.isBefore(start)) null else Period.between(start, day)
    }

    /** Parses a yyyyMMdd day code, or returns null if it is not a valid date. */
    fun parseDayCode(dayCode: String): LocalDate? {
        return try {
            LocalDate.parse(dayCode, DAY_CODE_FORMATTER)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun toDayCode(date: LocalDate): String = date.format(DAY_CODE_FORMATTER)

    /** A label's start date as shown in settings, in the app's language. */
    fun formatDate(resources: Resources, date: LocalDate): String {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(resources.configuration.locales[0])
            .format(date)
    }

    /** The text for [label] on [dayCode], or null if that day is before the label's start date. */
    fun getText(resources: Resources, label: DayLabel, dayCode: String): String? {
        val start = parseDayCode(label.startDayCode) ?: return null
        val day = parseDayCode(dayCode) ?: return null
        val period = elapsed(start, day) ?: return null
        val parts = buildList {
            if (period.years > 0) {
                add(resources.getQuantityString(org.fossify.commons.R.plurals.years, period.years, period.years))
            }
            if (period.months > 0) {
                add(resources.getQuantityString(org.fossify.commons.R.plurals.months, period.months, period.months))
            }
            if (period.days > 0 || isEmpty()) {
                add(resources.getQuantityString(org.fossify.commons.R.plurals.days, period.days, period.days))
            }
        }

        return resources.getString(R.string.day_label_text, label.title, parts.joinToString(", "))
    }

    fun toJson(labels: List<DayLabel>): String {
        val array = JSONArray()
        labels.forEach {
            array.put(JSONObject().put(KEY_TITLE, it.title).put(KEY_START, it.startDayCode))
        }
        return array.toString()
    }

    /** Parses stored labels, skipping entries that are malformed or have an invalid start date. */
    fun fromJson(json: String): List<DayLabel> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val title = item.optString(KEY_TITLE).trim()
                val start = item.optString(KEY_START)
                if (title.isEmpty() || parseDayCode(start) == null) null else DayLabel(title, start)
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
