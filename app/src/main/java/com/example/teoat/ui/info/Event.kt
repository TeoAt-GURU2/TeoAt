package com.example.teoat.ui.info

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Event(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val host: String = "",          
    val location: String = "",
    val region: Long = 0L,          // 지역 코드 (숫자)
    val target: String = "",       // 행사 대상(연령) 
    val startDate: Timestamp? = null, // 등록 시작일 (타임스탬프)
    val endDate: Timestamp? = null,

    @get:Exclude @set:Exclude
    var isScrapped: Boolean = false
)
