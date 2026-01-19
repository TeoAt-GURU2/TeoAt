package com.example.teoat.ui.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("GDreamCard")
    fun getStores(
        @Query("KEY") key: String,
        @Query("Type") type: String = "json",
        @Query("pIndex") index: Int = 1,
        @Query("pSize") size: Int = 100 // 한 번에 가져올 데이터 수
    ): Call<StoreResponse>
}