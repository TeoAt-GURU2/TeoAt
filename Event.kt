package com.example.teoat.data.model

data class Event(
    val event_id: String = "",      // Firestore 문서 ID
    val title: String = "",         // 행사명
    val description: String = "",   // 상세 내용
    val target_age: String = "",    // 대상 연령 (예: "20")
    val region: String = "",        // 지역 (예: "서울")
    val status: String = "신청중",   // 상태
    var isScrapped: Boolean = false // 스크랩 여부
)