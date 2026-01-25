package com.example.teoat.data.model

import com.google.firebase.Timestamp

data class NotificationModel(
    var id      : String = "",
    var title   : String = "",  // 행사 제목
    var message : String = "",  // 알림 내용
    var eventId : String = "",  // 해당 행사 ID (클릭 시 이동을 위해서)0
    var timestamp: Timestamp = Timestamp.now(), // 알림 생성 시간
    var isRead  : Boolean = false // 읽음 여부
)
