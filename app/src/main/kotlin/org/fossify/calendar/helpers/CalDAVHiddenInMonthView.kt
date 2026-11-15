package org.fossify.calendar.helpers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.ExtendedProperties
import android.widget.Toast
import org.fossify.calendar.R
import org.fossify.calendar.extensions.queryCursorInlined
import org.fossify.calendar.models.CalDAVCalendar
import org.fossify.calendar.models.Event
import org.fossify.commons.extensions.getLongValue
import org.fossify.commons.extensions.getStringValue
import org.fossify.commons.extensions.toast
import org.json.JSONArray
import org.json.JSONException

/**
 * "Hide in monthly view" for CalDAV events, stored in the event itself as X-FOSSIFY-HIDE-IN-MONTH-VIEW:TRUE so it
 * syncs to other devices.
 *
 * DAVx5 keeps iCalendar properties it does not know as extended properties named [DAVX5_UNKNOWN_PROPERTY] with a JSON
 * value `[name, value, {params}]`, and writes them back into the event when uploading. Android only lets sync adapters
 * write extended properties, so [store] writes the row on behalf of DAVx5 and then marks the event dirty so DAVx5
 * uploads it. Reading them is allowed for any app with calendar access.
 */
class CalDAVHiddenInMonthView(private val context: Context) {

    fun isHidden(eventId: Long): Boolean = getPropertyIds(eventId).isNotEmpty()

    /** Brings the event's extended properties in line with [Event.isHiddenInMonthView]; looks up the calendar only if needed. */
    fun store(event: Event, findCalendar: () -> CalDAVCalendar?) {
        val eventId = event.getCalDAVEventId()
        val existing = getPropertyIds(eventId)
        val hidden = event.isHiddenInMonthView()
        if (hidden == existing.isNotEmpty()) {
            return
        }

        val calendar = findCalendar()
        if (calendar == null || calendar.accountType != DAVX5_ACCOUNT_TYPE) {
            if (hidden) {
                context.toast(R.string.hide_in_monthly_view_needs_davx5, Toast.LENGTH_LONG)
            }
            return
        }

        try {
            val propertiesUri = asSyncAdapter(ExtendedProperties.CONTENT_URI, calendar)
            if (hidden) {
                val values = ContentValues().apply {
                    put(ExtendedProperties.EVENT_ID, eventId)
                    put(ExtendedProperties.NAME, DAVX5_UNKNOWN_PROPERTY)
                    put(ExtendedProperties.VALUE, JSONArray(listOf(HIDE_IN_MONTH_VIEW, "TRUE")).toString())
                }
                context.contentResolver.insert(propertiesUri, values)
            } else {
                val placeholders = existing.joinToString(",") { "?" }
                context.contentResolver.delete(
                    propertiesUri,
                    "${ExtendedProperties._ID} IN ($placeholders)",
                    existing.map { it.toString() }.toTypedArray()
                )
            }

            // written as the sync adapter, so the provider did not flag the event for upload: do it explicitly
            val dirty = ContentValues().apply { put(Events.DIRTY, 1) }
            val eventUri = asSyncAdapter(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), calendar)
            context.contentResolver.update(eventUri, dirty, null, null)
        } catch (_: Exception) {
            context.toast(R.string.hide_in_monthly_view_sync_failed, Toast.LENGTH_LONG)
        }
    }

    private fun getPropertyIds(eventId: Long): List<Long> {
        val ids = ArrayList<Long>()
        val projection = arrayOf(ExtendedProperties._ID, ExtendedProperties.VALUE)
        val selection = "${ExtendedProperties.EVENT_ID} = ? AND ${ExtendedProperties.NAME} = ?"
        val selectionArgs = arrayOf(eventId.toString(), DAVX5_UNKNOWN_PROPERTY)
        context.queryCursorInlined(ExtendedProperties.CONTENT_URI, projection, selection, selectionArgs) { cursor ->
            val value = cursor.getStringValue(ExtendedProperties.VALUE) ?: return@queryCursorInlined
            try {
                val property = JSONArray(value)
                if (property.optString(0) == HIDE_IN_MONTH_VIEW && property.optString(1).equals("TRUE", true)) {
                    ids.add(cursor.getLongValue(ExtendedProperties._ID))
                }
            } catch (_: JSONException) {
                // a malformed or foreign property, not ours
            }
        }
        return ids
    }

    private fun asSyncAdapter(uri: Uri, calendar: CalDAVCalendar): Uri {
        return uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, calendar.accountName)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, calendar.accountType)
            .build()
    }
}
