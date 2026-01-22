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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FacilityActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var adapter: FacilityAdapter
    private var allFacilities = mutableListOf<Facility>()
    private var isFavoriteMode = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markerMap = HashMap<String, Marker>()

    // Firebase 변수
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
                val user = auth.currentUser
                if (user == null) {
                    Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
                    return@FacilityAdapter
                }

                // 상태 반전 및 Firestore 업데이트
                facility.isFavorite = !facility.isFavorite
                toggleFavoriteInFirestore(user.uid, facility)

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
        val myApiKey = BuildConfig.MCS_API_KEY

        apiService.getFacilities(key = myApiKey).enqueue(object : Callback<FacilityResponse> {
            override fun onResponse(call: Call<FacilityResponse>, response: Response<FacilityResponse>) {
                if (response.isSuccessful) {
                    // 1. 기존 리스트 초기화 (중복 방지)
                    allFacilities.clear()

                    // 2. API로부터 받은 데이터를 Facility 객체로 변환하여 리스트에 담기
                    val items = response.body()?.Youngbgfacltinst?.get(1)?.row
                    items?.forEach { item ->
                        allFacilities.add(Facility(
                            id = item.INST_NM ?: "", // 기관명을 고유 ID로 사용
                            name = item.INST_NM ?: "이름 없음",
                            address = item.REFINE_ROADNM_ADDR ?: "주소 없음",
                            phone = item.TELNO ?: "번호 없음",
                            latitude = item.REFINE_WGS84_LAT?.toDoubleOrNull() ?: 37.0,
                            longitude = item.REFINE_WGS84_LOGT?.toDoubleOrNull() ?: 127.0
                        ))
                    }

                    // 3. [중요] 데이터를 다 불러온 후, 로그인 상태라면 즐겨찾기 동기화 시작
                    if (auth.currentUser != null) {
                        syncFavorites()
                    } else {
                        // 로그인 안 된 상태라면 바로 화면 갱신
                        refreshUI()
                    }
                }
            }

            override fun onFailure(call: Call<FacilityResponse>, t: Throwable) {
                Toast.makeText(this@FacilityActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // UI 갱신 로직을 별도 함수로 분리하여 재사용성 높임
    private fun refreshUI() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            addMarkersToMap(allFacilities)
        }
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

    //Firestore에 즐겨찾기 저장/삭제
    private fun toggleFavoriteInFirestore(userId: String, facility: Facility) {
        val favRef = db.collection("users").document(userId)
            .collection("favorites").document(facility.id)

        if (facility.isFavorite) {
            val data = hashMapOf(
                "name" to facility.name,
                "address" to facility.address,
                "id" to facility.id
            )
            favRef.set(data)
        } else {
            favRef.delete()
        }
    }

    //내 즐겨찾기 목록을 가져와서 API 데이터와 동기화
    private fun syncFavorites() {
        val user = auth.currentUser ?: return

        // Firestore에서 해당 사용자의 즐겨찾기 컬렉션 전체 가져오기
        db.collection("users").document(user.uid).collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                // 저장된 즐겨찾기 문서들의 ID(시설명) 리스트 추출
                val favoriteIds = documents.map { it.id }

                // API로 불러온 전체 리스트를 돌면서, 내 즐겨찾기에 포함된 시설은 isFavorite를 true로 변경
                allFacilities.forEach { facility ->
                    if (favoriteIds.contains(facility.id)) {
                        facility.isFavorite = true
                    }
                }
                // 동기화 완료 후 화면 갱신
                refreshUI()
            }
            .addOnFailureListener {
                // 실패 시에도 일단 화면은 보여줌
                refreshUI()
            }
    }
}