package com.example.teoat.ui.map

data class StoreResponse(
    val GDreamCard: List<GDreamCard>?
)

data class GDreamCard(
    val row: List<StoreItem>?
)

data class StoreItem(
    val FACLT_NM: String?,        // 가맹점명
    val DIV_NM: String?,          // 구분(분류)
    val REFINE_ROADNM_ADDR: String?, // 소재지도로명주소
    val REFINE_WGS84_LAT: String?,   // WGS84위도
    val REFINE_WGS84_LOGT: String?   // WGS84경도
)