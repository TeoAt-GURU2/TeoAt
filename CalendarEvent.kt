package com.example.teoat.data.model

data class CalendarEvent(
    val calendar_id: Int = 0,    // 일정 ID
    val user_id: Int = 0,        // 사용자 ID
    val title: String = "",       // 일정 제목
    val event_date: String = ""   // 행사 날짜
)
