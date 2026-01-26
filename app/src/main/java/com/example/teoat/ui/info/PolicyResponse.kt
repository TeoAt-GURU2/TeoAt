package com.example.teoat.ui.info

import com.google.gson.annotations.SerializedName

data class PolicyResponse(
    val JnfrMiryfcSsrsM: List<PolicyWrapper>?
)

data class PolicyWrapper(
    val row: List<PolicyItem>?
)

data class PolicyItem(
    val TITLE: String?,         // 사업명
    val MAIN_PURPS: String?,    // 주목적
    val OPERT_MAINBD_NM: String?, // 운영주체명
    val OPERT_ORGNZT_NM: String?, // 운영조직명
    val GUID: String?,          // 안내: 문의처
    val RELATE_INFO: String?       // 관련 정보: 상세페이지 URL
)