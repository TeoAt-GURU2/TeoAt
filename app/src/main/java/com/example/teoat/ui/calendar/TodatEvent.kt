package com.example.teoat.ui.calendar

data class TodayEvent(
    val title: String = "",
    val location: String = "",
    val date: String = "",   // "yyyy-MM-dd"
    val status: String = ""  // 예: "신청중"
)
