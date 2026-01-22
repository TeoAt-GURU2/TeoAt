package com.example.teoat.data.model

import com.google.firebase.Timestamp

data class NotificationModel(
    val id      : String = "",
    val title   : String = "",  // 행사 제목
    val message : String = "",  // 알림 내용
    val eventId : String = "",  // 해당 행사 ID (클릭 시 이동을 위해서)0
    val timestamp: Timestamp = Timestamp.now(), // 알림 생성 시간
    val isRead  : Boolean = false // 읽음 여부
)
