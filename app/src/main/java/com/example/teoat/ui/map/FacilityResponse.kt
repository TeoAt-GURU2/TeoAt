package com.example.teoat.ui.map

data class FacilityResponse(
    val Youngbgfacltinst: List<Youngbgfacltinst>?
)

data class Youngbgfacltinst(
    val row: List<FacilityItem>?
)

data class FacilityItem(
    val INST_NM: String?,            // 기관명  
    val TELNO: String?,              // 전화번호  
    val REFINE_ROADNM_ADDR: String?, // 소재지도로명주소  
    val REFINE_WGS84_LAT: String?,   // WGS84위도  
    val REFINE_WGS84_LOGT: String?   // WGS84경도  
)
