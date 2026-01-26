package com.example.teoat.ui.info

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Event(
    // id는 DB 필드엔 없지만, 문서 ID를 저장하기 위해 남겨둠 (Exclude 유지)
    @get:Exclude @set:Exclude
    var id: String = "",

    val title: String = "",
    val description: String = "",
    val host: String = "",
    val location: String = "",
    val region: Long = 0L,
    val target: String = "",

    // 👇 파이어베이스의 'startDate', 'endDate' (Timestamp 타입)와 이름 일치!
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,

    // 👇 [수정] @Exclude 제거! (DB에 있는 값을 읽어와야 하므로)
    var isScrapped: Boolean = false
)