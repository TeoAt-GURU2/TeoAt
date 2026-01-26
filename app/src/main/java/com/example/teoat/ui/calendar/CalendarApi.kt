package com.example.teoat.ui.calendar

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApi {

    // GET https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events?...&key=API_KEY
    @GET("calendar/v3/calendars/0be8deb01781b00a97bace95281bfc4eb7bed1d674606f2ca58899b25c2f687f@group.calendar.google.com/events")
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
