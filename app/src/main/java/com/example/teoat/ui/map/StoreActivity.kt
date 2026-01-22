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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.teoat.base.BaseActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StoreActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var adapter: StoreAdapter

    //전체 데이터 원본 (API에서 받아 온 모든 데이터)
    private var allStores = mutableListOf<Store>()

    //현재 지도 화면에 보이는 데이터 (지도 이동 시에 갱신됨)
    private var currentVisibleStores = mutableListOf<Store>()
    private var isFavoriteMode = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markerMap = HashMap<String, com.google.android.gms.maps.model.Marker>()

    // Firebase 변수
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. 뷰 연결
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
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()

                } else {
                    // 상태 반전 및 DB 업데이트
                    store.isFavorite = !store.isFavorite
                    toggleFavoriteInFirestore(currentUser.uid, store)
                    applyFilters(etSearch.text.toString())
                }
            },
            onItemClick = { store ->
                val location = LatLng(store.latitude, store.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
                val marker = markerMap[store.id]
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
        // 아이콘 클릭 시에만 검색되게 하려면 아래 블록을 삭제하시면 됩니다.
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters(s?.toString() ?: "")
            }
        })

        // 5. 버튼 클릭 리스너 - 즐겨찾기 버튼
        ivTopFavorite.setOnClickListener {
            isFavoriteMode = !isFavoriteMode
            applyFilters(etSearch.text.toString())
        }

        // 5. 버튼 클릭 리스너 - 내 위치 버튼
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

    private fun loadApiData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.gg.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val myApiKey = BuildConfig.MCS_API_KEY

        apiService.getStores(key = myApiKey).enqueue(object : retrofit2.Callback<StoreResponse> {
            override fun onResponse(call: retrofit2.Call<StoreResponse>, response: retrofit2.Response<StoreResponse>) {
                if (response.isSuccessful) {
                    // 1. 기존 리스트 초기화 (중복 방지)
                    allStores.clear()

                    // 2. API 데이터를 Store 모델로 변환하여 리스트에 추가
                    val items = response.body()?.GDreamCard?.get(1)?.row
                    items?.forEach { item ->
                        allStores.add(Store(
                            id = item.FACLT_NM ?: "",
                            name = item.FACLT_NM ?: "이름 없음",
                            address = item.REFINE_ROADNM_ADDR ?: "주소 없음",
                            latitude = item.REFINE_WGS84_LAT?.toDoubleOrNull() ?: 37.0,
                            longitude = item.REFINE_WGS84_LOGT?.toDoubleOrNull() ?: 127.0
                        ))
                    }

                    // 3. 데이터를 다 불러온 후, 로그인 상태라면 즐겨찾기 동기화 시작
                    if (auth.currentUser != null) {
                        syncFavorites() // Firestore에서 내 즐겨찾기 목록을 가져와 비교
                    } else {
                        // 로그인 안 된 상태라면 바로 화면 갱신
                        refreshUI()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<StoreResponse>, t: Throwable) {
                Toast.makeText(this@StoreActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // UI 갱신 로직 분리 (Adapter와 Map 마커 업데이트)
    private fun refreshUI() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            addMarkersToMap(allStores)
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
    // Firestore 저장/삭제 함수
    private fun toggleFavoriteInFirestore(userId: String, store: Store) {
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
}
