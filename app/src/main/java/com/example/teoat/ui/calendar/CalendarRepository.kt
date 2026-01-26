package com.example.teoat.ui.calendar

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

        val res = api.listEvents(
            calendarId = calendarId,
            apiKey = apiKey,
            timeMin = timeMin,
            timeMax = timeMax
        )

        return res.items ?: emptyList()
    }
}
