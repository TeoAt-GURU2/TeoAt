package com.example.teoat.ui.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    // 1. 기존 급식카드 가맹점 API
    @GET("GDreamCard")
    fun getStores(
        @Query("KEY") key: String,
        @Query("Type") type: String = "json",
        @Query("pIndex") index: Int = 1,
        @Query("pSize") size: Int = 100
    ): Call<StoreResponse>

    // 2. 청소년 시설 기관 현황 API 추가
    @GET("Youngbgfacltinst")
    fun getFacilities(
        @Query("KEY") key: String,
        @Query("Type") type: String = "json",
        @Query("pIndex") index: Int = 1,
        @Query("pSize") size: Int = 100
    ): Call<FacilityResponse>
}