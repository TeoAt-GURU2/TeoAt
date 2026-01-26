package com.example.teoat.ui.map

import com.example.teoat.ui.info.PolicyResponse
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

    // 2. 청소년 시설 기관 현황 API
    @GET("Youngbgfacltinst")
    fun getFacilities(
        @Query("KEY") key: String,
        @Query("Type") type: String = "json",
        @Query("pIndex") index: Int = 1,
        @Query("pSize") size: Int = 100
    ): Call<FacilityResponse>

    // 3. 청소년관련중앙부처복지서비스 현황 API
    @GET("JnfrMiryfcSsrsM")
    fun getPolicies(
        @Query("KEY") key: String,
        @Query("Type") type: String = "json",
        @Query("pIndex") index: Int = 1,
        @Query("pSize") size: Int = 100
    ) : Call<PolicyResponse>
}