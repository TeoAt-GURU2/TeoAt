package com.example.teoat.ui.calendar

import com.google.gson.annotations.SerializedName

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
    // 시간 있는 일정
    @SerializedName("dateTime") val dateTime: String?,
    // 종일 일정
    @SerializedName("date") val date: String?
)
