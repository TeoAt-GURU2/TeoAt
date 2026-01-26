package com.example.teoat.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.teoat.BuildConfig
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityPolicyBinding
import com.example.teoat.ui.map.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PolicyActivity : BaseActivity() {
    // ViewBinding 객체 선언
    private lateinit var binding: ActivityPolicyBinding

    private lateinit var adapter: PolicyAdapter
    private var policyList = mutableListOf<PolicyItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding 초기화
        binding = ActivityPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 어댑터 설정, 클릭 이벤트 정의
        adapter = PolicyAdapter(policyList) { policy ->
            // 자세히 보기 버튼 클릭 시에 동작: HMPG_ADR(관련 복지로 웹페이지)로 이동
            val url = policy.RELATE_INFO

            if (!url.isNullOrEmpty()) {
                try {
                    // http 또는 https 프로토콜이 없는 경우 붙여주기
                    val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        "http://$url"
                    } else {
                        url
                    }

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "상세 페이지 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // RecyclerView 설정
        binding.rvPolicyList.layoutManager = LinearLayoutManager(this)
        binding.rvPolicyList.adapter = adapter

        loadPolicyData()
    }

    private fun loadPolicyData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.gg.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val myApiKey = BuildConfig.MCS_API_KEY

        apiService.getPolicies(key = myApiKey).enqueue(object : Callback<PolicyResponse> {
            override fun onResponse(call: Call<PolicyResponse>, response: Response<PolicyResponse>) {
                if (response.isSuccessful) {
                    // 응답 구조: JnfrMiryfcSsrsM -> [Head, Row]
                    val wrapperList = response.body()?.JnfrMiryfcSsrsM

                    // 일반적으로 API 응답 리스트의 두 번째 요소(index 1)에 실제 데이터 row가 존재
                    val items = wrapperList?.getOrNull(1)?.row

                    if (items != null) {
                        // 디버깅용
                        items.forEach { android.util.Log.d("API TEST", "제목: ${it.TITLE}, URL값: ${it.RELATE_INFO}") }

                        policyList.clear()
                        policyList.addAll(items)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@PolicyActivity, "검색된 복지 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PolicyActivity, "서버 응답 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PolicyResponse>, t: Throwable) {
                Toast.makeText(this@PolicyActivity, "데이터 로드 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}