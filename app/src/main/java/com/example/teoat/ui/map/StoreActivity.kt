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
import com.example.teoat.BuildConfig
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.teoat.base.BaseActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.internal.notify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class StoreActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var isLoading = false
    private lateinit var adapter: StoreAdapter

    //전체 데이터 원본 (API에서 받아 온 모든 데이터)
    private var allStores = mutableListOf<Store>()


    private var isFavoriteMode = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markerMap = HashMap<String, com.google.android.gms.maps.model.Marker>()

    // Firestore 변수 선언: 로그인 확인 여부, 즐겨찾기 연동을 위함
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. 뷰 연결
        val rvStoreList = findViewById<RecyclerView>(R.id.rv_store_list)
        etSearch = findViewById<EditText>(R.id.et_search)
        val ivTopFavorite = findViewById<ImageView>(R.id.iv_top_favorite)
        val ivMyLocation = findViewById<ImageView>(R.id.iv_my_location)

        // 2. 지도 설정
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 3. 어댑터 설정
        adapter = StoreAdapter(allStores,
            onFavoriteClick = { store ->
                // 로그인 여부 확인
                val currentUser = auth.currentUser
                if (currentUser == null ) {
                    Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
                    return@StoreAdapter
                } else {
                    // DB 업데이트
                    store.isFavorite = !store.isFavorite
                    toggleFavoriteInFirestore(currentUser.uid, store)
                    // 상태 변경 후 UI 갱신
                    updateListBasedOnMap()
                }
            },
            onItemClick = { store ->
                val location = LatLng(store.latitude, store.longitude)
                // 1. 지도를 해당 위치로 이동
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

                // 2. 해당 위치의 마커를 찾아서 정보창(이름) 띄우기
                // 기존에 추가된 마커들 중에서 좌표가 일치하는 마커를 찾아 정보를 표시합니다.
                val marker = markerMap[store.id] // 마커를 관리하는 Map이 필요합니다.
                marker?.showInfoWindow()
            }
        )
        rvStoreList.layoutManager = LinearLayoutManager(this)
        rvStoreList.adapter = adapter

        // 4. 검색창 텍스트 감시
        // 돋보기 아이콘 터치 시 검색 실행
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

        // 키보드에서 '검색(돋보기)' 버튼을 눌렀을 때 실행
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

        // 실시간 검색 결과 보이기
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateListBasedOnMap()
            }
        })

        // 5. 버튼 클릭 리스너 - 즐겨찾기 버튼
        ivTopFavorite.setOnClickListener {
            val currentUSer = auth.currentUser
            if (currentUSer != null) {
                isFavoriteMode = !isFavoriteMode
                updateListBasedOnMap()
            } else {
                Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. 버튼 클릭 리스너 - 내 위치 버튼
        ivMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 지도가 멈출 때마다 목록 갱신
        mMap.setOnCameraIdleListener {
            updateListBasedOnMap()
        }
        loadAllApiData(pageIndex = 1)
    }

    // [통합된 메서드] 지도 영역 + 검색어 + 즐겨찾기 필터링 및 정렬 수행
    private fun updateListBasedOnMap() {
        if (!::mMap.isInitialized || allStores.isEmpty()) return

        // 1. 현재 지도 영역(Bounds)과 중심 좌표 가져오기
        val visibleRegion = mMap.projection.visibleRegion
        val bounds = visibleRegion.latLngBounds
        val center = mMap.cameraPosition.target

        // 2. 검색어 가져오기
        val query = etSearch.text.toString()

        // 3. 필터링 (지도 영역 안 && 검색어 포함 && 즐겨찾기 모드)
        val filteredStores = allStores.filter { store ->
            val storeLatLng = LatLng(store.latitude, store.longitude)
            val inBounds = bounds.contains(storeLatLng)
            val matchesQuery = store.name.contains(query, ignoreCase = true)
            val matchesFavorite = if (isFavoriteMode) store.isFavorite else true

            inBounds && matchesQuery && matchesFavorite
        }

        // 4. 거리순 정렬
        val sortedStores = filteredStores.sortedBy { store ->
            val result = FloatArray(1)
            Location.distanceBetween(
                center.latitude, center.longitude,
                store.latitude, store.longitude,
                result
            )
            result[0]
        }

        // 5. 어댑터 갱신
        adapter.updateData(sortedStores)

        // 6. 마커 갱신 (성능을 위해 최대 100개만 표시)
        addMarkersToMap(sortedStores.take(40))
    }

    private fun loadAllApiData(pageIndex: Int) {
        if (isLoading && pageIndex == 1) return
        isLoading = true

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.gg.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val myApiKey = BuildConfig.MCS_API_KEY

        apiService.getStores(key = myApiKey, index = pageIndex, size = 1000)
            .enqueue(object : Callback<StoreResponse> {
                override fun onResponse(call: Call<StoreResponse>, response: Response<StoreResponse>) {
                    if (response.isSuccessful) {
                        val items = response.body()?.GDreamCard?.get(1)?.row

                        if (!items.isNullOrEmpty()) {
                            // 가져온 데이터를 리스트에 추가
                            items.forEach { item ->
                                // 좌표가 있는 데이터만 저장 (유효성 검사)
                                val lat = item.REFINE_WGS84_LAT?.toDoubleOrNull()
                                val lng = item.REFINE_WGS84_LOGT?.toDoubleOrNull()

                                if (lat != null && lng != null) {
                                    allStores.add(Store(
                                        id = item.FACLT_NM ?: "",
                                        name = item.FACLT_NM ?: "이름 없음",
                                        address = item.REFINE_ROADNM_ADDR ?: "주소 없음",
                                        latitude = lat,   // API가 준 위도 활용
                                        longitude = lng,  // API가 준 경도 활용
                                        isFavorite = false
                                    ))
                                }
                            }

                            // [중요] 다음 페이지가 있는지 확인하기 위해 계속 호출
                            // 1000개를 꽉 채워 받았다면 다음 페이지가 있을 확률이 높음
                            if (items.size == 1000) {
                                loadAllApiData(pageIndex + 1)
                            } else {
                                // 데이터 없음 -> 완료 처리
                                finishLoading()
                            }
                        } else {
                            // 실패해도 일단 완료 처리
                            finishLoading()
                        }
                    }
                }

                override fun onFailure(call: Call<StoreResponse>, t: Throwable) {
                    finishLoading()
                    Toast.makeText(this@StoreActivity, "데이터 로딩 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // [2] 현재 지도 화면(사각형 영역) 안에 있는 가게만 골라내는 함수
    private fun updateMapMarkersByScreen() {
        if (!::mMap.isInitialized || allStores.isEmpty()) return

        // 현재 보고 있는 지도의 영역(Lat/Lon 경계)을 가져옴
        val bounds = mMap.projection.visibleRegion.latLngBounds
        val center = mMap.cameraPosition.target

        // [필터링] API에서 받은 좌표(store.latitude, longitude)가 현재 지도 영역(bounds) 안에 있는지 확인
        val screenStores = allStores.filter { store ->
            val storePos = LatLng(store.latitude, store.longitude)
            bounds.contains(storePos) // 이 함수가 핵심! (영역 안에 포함되면 true)
        }

        // [정렬] 화면 중심에서 가까운 순서대로 정렬
        val sortedStores = screenStores.sortedBy { store ->
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                center.latitude, center.longitude,
                store.latitude, store.longitude,
                result
            )
            result[0]
        }

        // 검색어가 있다면 검색어 필터도 적용
        val query = findViewById<EditText>(R.id.et_search).text.toString()
        val finalStores = sortedStores.filter {
            it.name.contains(query, ignoreCase = true) &&
                    (!isFavoriteMode || it.isFavorite)
        }

        // 화면 갱신
        adapter.updateData(finalStores)

        // 마커가 너무 많으면 렉 걸릴 수 있으므로 100개까지만 지도에 표시
        addMarkersToMap(finalStores.take(100))
    }

    private fun finishLoading() {
        isLoading = false
        if (auth.currentUser != null) {
            syncFavorites()
        } else {
            runOnUiThread { updateListBasedOnMap() }
        }
        runOnUiThread {
            Toast.makeText(this@StoreActivity, "가맹점 데이터 로드 완료", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToCurrentLocation() {
        // 1. 위치 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없다면 요청 (이미 요청 코드가 있다면 생략 가능)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        // 2. 현재 위치 가져오기
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // 3. 지도를 현재 위치로 이동 (줌 레벨 15f)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                // 필요하다면 현재 위치에 마커 추가
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("내 위치"))
            } else {
                Toast.makeText(this, "현재 위치를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 내 즐겨찾기 동기화 함수 추가
    private fun syncFavorites() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).collection("favorite_stores")
            .get()
            .addOnSuccessListener { documents ->
                val favoriteIds = documents.map { it.id }
                allStores.forEach { store ->
                    if (favoriteIds.contains(store.id)) {
                        store.isFavorite = true
                    }
                }
                refreshUI()
            }
            .addOnFailureListener {
                refreshUI()
            }
    }

    // 즐겨찾기 설정 : Firestore에서 저장, 삭제하기 위한 함수
    private fun toggleFavoriteInFirestore(userId : String, store: Store) {
        // 가맹점(stores) 컬렉션에 저장
        val favRef = db.collection("users").document(userId)
            .collection("favorite_stores").document(store.id)

        if (store.isFavorite) {
            val data = hashMapOf(
                "name" to store.name,
                "address" to store.address,
                "id" to store.id
            )
            favRef.set(data)
        } else {
            favRef.delete()
        }
    }

    private fun applyFilters(query: String) {
        val filtered = allStores.filter { store ->
            // 이름에 검색어가 포함되는지만 확인
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
        markerMap.clear() // 기존 매핑 데이터 초기화

        for (store in stores) {
            val position = LatLng(store.latitude, store.longitude)
            val markerOptions = MarkerOptions()
                .position(position)
                .title(store.name)
                .snippet(store.address) // 주소도 함께 나오게 하고 싶다면 추가

            val marker = mMap.addMarker(markerOptions)

            // 마커 객체를 ID를 키값으로 저장
            if (marker != null) {
                markerMap[store.id] = marker
            }
        }
    }

    // UI 갱신 로직 분리
    private fun refreshUI() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            addMarkersToMap(allStores)
        }
    }
}