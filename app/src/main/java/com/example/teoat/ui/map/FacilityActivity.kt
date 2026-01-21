package com.example.teoat.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.BuildConfig
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityFacilityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FacilityActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var adapter: FacilityAdapter
    private var allFacilities = mutableListOf<Facility>()
    private var isFavoriteMode = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markerMap = HashMap<String, Marker>()

    // 바인딩 변수 선언
    private lateinit var binding: ActivityFacilityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. 뷰 연결 삭제 -> binding 객체 사용으로 변경
        binding = ActivityFacilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. 지도 설정
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadApiData()

        // 3. 어댑터 설정
        adapter = FacilityAdapter(allFacilities,
            onFavoriteClick = { facility ->
                facility.isFavorite = !facility.isFavorite
                applyFilters(binding.etSearch.text.toString())
            },
            onItemClick = { facility ->
                val location = LatLng(facility.latitude, facility.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

                val marker = markerMap[facility.id]
                marker?.showInfoWindow()
            }
        )
        binding.rvFacilityList.layoutManager = LinearLayoutManager(this)
        binding.rvFacilityList.adapter = adapter

        // 4. 검색창 터치 리스너 (돋보기 아이콘 클릭 시 검색)
        binding.etSearch.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableRight = binding.etSearch.compoundDrawables[2]
                if (drawableRight != null && event.rawX >= (binding.etSearch.right - drawableRight.bounds.width() - binding.etSearch.paddingEnd)) {
                    v.performClick()
                    applyFilters(binding.etSearch.text.toString())
                    return@setOnTouchListener true
                }
            }
            false
        }

        // 키보드 검색 버튼 대응
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters(binding.etSearch.text.toString())
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        // 실시간 검색 텍스트 감시
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters(s?.toString() ?: "")
            }
        })

        // 5. 버튼 클릭 리스너
        binding.ivTopFavorite.setOnClickListener {
            isFavoriteMode = !isFavoriteMode
            applyFilters(binding.etSearch.text.toString())
        }

        binding.ivMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addMarkersToMap(allFacilities)
    }

    private fun applyFilters(query: String) {
        val filtered = allFacilities.filter { facility ->
            val matchesQuery = facility.name.contains(query, ignoreCase = true)
            val matchesFavorite = if (isFavoriteMode) facility.isFavorite else true
            matchesQuery && matchesFavorite
        }
        adapter.updateData(filtered)
        addMarkersToMap(filtered)
    }

    private fun addMarkersToMap(facilities: List<Facility>) {
        if (!::mMap.isInitialized) return
        mMap.clear()
        markerMap.clear()

        for (facility in facilities) {
            val position = LatLng(facility.latitude, facility.longitude)
            val markerOptions = MarkerOptions()
                .position(position)
                .title(facility.name)
                .snippet(facility.address)

            val marker = mMap.addMarker(markerOptions)
            if (marker != null) {
                markerMap[facility.id] = marker
            }
        }
    }

    private fun loadApiData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.gg.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val myApiKey = BuildConfig.MCS_API_KEY // BuildConfig에 키가 설정되어 있어야 합니다.

        // 청소년시설 현황 API 호출 (Youngbgfacltinst)
        apiService.getFacilities(key = myApiKey).enqueue(object : Callback<FacilityResponse> {
            override fun onResponse(call: Call<FacilityResponse>, response: Response<FacilityResponse>) {
                if (response.isSuccessful) {
                    // API 문서 구조에 맞게 수정 (Youngbgfacltinst 경로 확인 필요)
                    val items = response.body()?.Youngbgfacltinst?.get(1)?.row
                    items?.forEach { item ->
                        allFacilities.add(Facility(
                            id = item.INST_NM ?: "", // 기관명
                            name = item.INST_NM ?: "이름 없음",
                            address = item.REFINE_ROADNM_ADDR ?: "주소 없음",
                            phone = item.TELNO ?: "번호 없음", // 전화번호 추가
                            latitude = item.REFINE_WGS84_LAT?.toDoubleOrNull() ?: 37.0,
                            longitude = item.REFINE_WGS84_LOGT?.toDoubleOrNull() ?: 127.0
                        ))
                    }
                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                        addMarkersToMap(allFacilities)
                    }
                }
            }

            override fun onFailure(call: Call<FacilityResponse>, t: Throwable) {
                Toast.makeText(this@FacilityActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("내 위치"))
            } else {
                Toast.makeText(this, "현재 위치를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}