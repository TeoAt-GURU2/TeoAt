package com.example.teoat.ui.map

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class StoreActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var adapter: StoreAdapter
    private var allStores = mutableListOf<Store>()
    private var isFavoriteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        // 1. 뷰 연결 (XML의 ID와 일치해야 오류가 사라집니다)
        val rvStoreList = findViewById<RecyclerView>(R.id.rv_store_list)
        val etSearch = findViewById<EditText>(R.id.et_search)
        val ivTopFavorite = findViewById<ImageView>(R.id.iv_top_favorite)
        val ivMyLocation = findViewById<ImageView>(R.id.iv_my_location)

        // 2. 지도 설정
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadApiData()

        // 3. 어댑터 설정
        adapter = StoreAdapter(allStores,
            onFavoriteClick = { store ->
                store.isFavorite = !store.isFavorite
                applyFilters(etSearch.text.toString())
            },
            onItemClick = { store ->
                val location = LatLng(store.latitude, store.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
            }
        )
        rvStoreList.layoutManager = LinearLayoutManager(this)
        rvStoreList.adapter = adapter

        // 4. 검색창 텍스트 감시 (오류 수정됨)
        // [방법 A] 돋보기 아이콘 터치 시 검색 실행
        etSearch.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                // 오른쪽 아이콘(돋보기) 영역 터치 감지
                val drawableRight = etSearch.compoundDrawables[2]
                if (drawableRight != null && event.rawX >= (etSearch.right - drawableRight.bounds.width() - etSearch.paddingEnd)) {
                    v.performClick()
                    applyFilters(etSearch.text.toString()) // 검색 실행
                    return@setOnTouchListener true
                }
            }
            false
        }

        // [방법 B] 키보드에서 '검색(돋보기)' 버튼을 눌렀을 때 실행
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                applyFilters(etSearch.text.toString())
                // 키보드 내리기 (선택 사항)
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        // (참고) 만약 실시간 검색도 계속 유지하고 싶다면 아래 코드를 남겨두세요.
        // 아이콘 클릭 시에만 검색되게 하려면 아래 블록을 삭제하시면 됩니다.
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters(s?.toString() ?: "")
            }
        })

        // 5. 버튼 클릭 리스너
        ivTopFavorite.setOnClickListener {
            isFavoriteMode = !isFavoriteMode
            applyFilters(etSearch.text.toString())
        }

        ivMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addMarkersToMap(allStores)
    }

    private fun applyFilters(query: String) {
        val filtered = allStores.filter { store ->
            val matchesQuery = store.name.contains(query, ignoreCase = true)
            val matchesFavorite = if (isFavoriteMode) store.isFavorite else true
            matchesQuery && matchesFavorite
        }
        adapter.updateData(filtered)
        addMarkersToMap(filtered)
    }

    private fun addMarkersToMap(stores: List<Store>) {
        if (!::mMap.isInitialized) return
        mMap.clear()
        for (store in stores) {
            val position = LatLng(store.latitude, store.longitude)
            mMap.addMarker(MarkerOptions().position(position).title(store.name))
        }
    }

    private fun loadApiData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.gg.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val myApiKey = BuildConfig.MCS_API_KEY

        // 본인의 API 키를 넣으세요 (샘플 키는 제한이 있을 수 있습니다)
        apiService.getStores(key = myApiKey).enqueue(object : retrofit2.Callback<StoreResponse> {
            override fun onResponse(call: retrofit2.Call<StoreResponse>, response: retrofit2.Response<StoreResponse>) {
                if (response.isSuccessful) {
                    val items = response.body()?.GDreamCard?.get(1)?.row
                    items?.forEach { item ->
                        // API 데이터를 우리 Store 모델로 변환
                        allStores.add(Store(
                            id = item.FACLT_NM ?: "",
                            name = item.FACLT_NM ?: "이름 없음",
                            category = item.DIV_NM ?: "분류 없음",
                            address = item.REFINE_ROADNM_ADDR ?: "주소 없음",
                            latitude = item.REFINE_WGS84_LAT?.toDoubleOrNull() ?: 37.0,
                            longitude = item.REFINE_WGS84_LOGT?.toDoubleOrNull() ?: 127.0
                        ))
                    }
                    // 데이터를 다 불러왔으면 리스트와 지도 갱신
                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                        addMarkersToMap(allStores)
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<StoreResponse>, t: Throwable) {
                // 실패 처리 (토스트 메시지 등)
            }
        })
    }

    private fun moveToCurrentLocation() {
        val myPos = LatLng(37.4979, 127.0276)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 15f))
    }
}