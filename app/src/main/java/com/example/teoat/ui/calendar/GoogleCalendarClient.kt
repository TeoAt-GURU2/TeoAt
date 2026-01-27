package com.example.teoat.ui.calendar

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.prolificinteractive.materialcalendarview.CalendarDay
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Models
data class CalendarEventsResponse(
    @SerializedName("items") val items: List<CalendarEventItem>?
)

data class CalendarEventItem(
    @SerializedName("id") val id: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("start") val start: CalendarEventTime?,
    @SerializedName("end") val end: CalendarEventTime?
)

data class CalendarEventTime(
    @SerializedName("dateTime") val dateTime: String?,
    @SerializedName("date") val date: String?
)

data class TodayEvent(
    val title: String = "",
    val location: String = "",
    val date: String = "",
    val status: String = ""
)

// API
interface CalendarApi {
    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun listEvents(
        @Path("calendarId") calendarId: String,
        @Query("key") apiKey: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("maxResults") maxResults: Int = 2500
    ): CalendarEventsResponse
}

// Repository
class CalendarRepository(
    private val api: CalendarApi,
    private val apiKey: String,
    private val calendarId: String
) {
    private val zone = ZoneId.of("Asia/Seoul")

    suspend fun fetchMonth(year: Int, month0Based: Int): List<CalendarEventItem> {
        val start = ZonedDateTime.of(year, month0Based + 1, 1, 0, 0, 0, 0, zone)
        val end = start.plusMonths(1)
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val timeMin = start.format(fmt)
        val timeMax = end.format(fmt)

        return try {
            val res = api.listEvents(calendarId, apiKey, timeMin, timeMax)
            res.items ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// Utils (날짜 변환)
object CalendarUtils {
    fun eventToCalendarDay(e: CalendarEventItem): CalendarDay? {
        val start = e.start ?: return null
        val millis = parseGoogleStartToMillis(start.dateTime, start.date) ?: return null

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        cal.timeInMillis = millis

        return CalendarDay.from(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun parseGoogleStartToMillis(dateTime: String?, dateOnly: String?): Long? {
        if (!dateTime.isNullOrBlank()) {
            return parseRfc3339(dateTime)
        }
        if (!dateOnly.isNullOrBlank()) {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                sdf.parse(dateOnly)?.time
            } catch (e: Exception) { null }
        }
        return null
    }

    private fun parseRfc3339(value: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val t = sdf.parse(value)?.time
                if (t != null) return t
            } catch (_: ParseException) { }
        }
        return null
    }
}