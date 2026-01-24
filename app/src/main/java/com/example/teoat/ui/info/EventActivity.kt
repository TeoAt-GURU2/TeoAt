package com.example.teoat.ui.info

import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.ui.main.MainActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EventActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()   // 사용자 인증 인스턴스

    // 전체 데이터와 현재 화면에 보여줄 데이터를 분리하여 관리
    private val allEventList = mutableListOf<Event>()
    private val scrappedEventIds = mutableSetOf<String>() // 내가 스크랩한 행사 ID 목록

    private lateinit var adapter: EventAdapter
    private lateinit var ivTopScrap: ImageView
    private var isFilterScrapOn = false     // 스크랩 모아보기 필터 상태

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_event_list)
        val editSearch = findViewById<EditText>(R.id.et_search)
        ivTopScrap = findViewById(R.id.iv_top_scrap)

        // 어댑터 설정, 클릭 리스너 포함
        adapter = EventAdapter(listOf()) { event ->
            toggleScrap(event)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 초기 데이터 로드
        loadUserDataAndEvents()

        // 검색어 입력 리스너 (실시간 필터링)
        editSearch.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 스크랩한 목록 모아보기 버튼
        ivTopScrap.setOnClickListener {
            isFilterScrapOn = !isFilterScrapOn

            if (isFilterScrapOn && auth.currentUser == null) {
                Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
                isFilterScrapOn = false
                return@setOnClickListener
            }
            updatedFilterIcon()
            filterList(editSearch.text.toString())
        }
    }

    private fun updatedFilterIcon() {
        if (isFilterScrapOn) {
            ivTopScrap.setImageResource(R.drawable.baseline_bookmark_24)
        } else {
            ivTopScrap.setImageResource(R.drawable.outline_bookmark_24)
        }
    }

    // 데이터 로드 영역
    private fun loadUserDataAndEvents() {
        val user = auth.currentUser
        if (user != null) {
            // 내 스크롭 목록 먼저 가져오기
            db.collection("users").document(user.uid).collection("scrap_events")
                .get()
                .addOnSuccessListener { documents ->
                    scrappedEventIds.clear()
                    for (doc in documents) {
                        scrappedEventIds.add(doc.id)
                    }

                    // 이 다음에 전체 행사 목록 가져오기
                    fetchEvents()
                }
                .addOnFailureListener {
                    Log.e("EventActivity", "스크랩 목록 로드 실패", it)
                    fetchEvents()
                }
        } else {
            scrappedEventIds.clear()
            fetchEvents()
        }


    }

    private fun fetchEvents() {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                allEventList.clear()
                for (document in result) {
                    val event = document.toObject(Event::class.java)
                    event.id = document.id

                    // 내 스크롭 목록에 있는지
                    event.isScrapped = scrappedEventIds.contains(event.id)
                    allEventList.add(event)
                }

                // 초기화면 갱신
                filterList("")
            }
            .addOnFailureListener { exception ->
                Log.e("EventActivity", "행사 목록 로드 실패", exception)
            }
    }

    // 필터링 로직
    private fun filterList(keyword: String) {
        val filtered = allEventList.filter { event ->
            // 검색어 필터
            val matchesKeyword = keyword.isEmpty() || event.title.contains(keyword, ignoreCase = true)
            // 스크랩 모아보기 필터
            val matchesScrap = if (isFilterScrapOn) event.isScrapped else true

            matchesKeyword && matchesScrap
        }
        adapter.updateData(filtered)
    }

    // 스크랩 기능
    private fun toggleScrap(event: Event) {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(user.uid)

        // UI 즉시 반영 (낙관적 업데이트)
        event.isScrapped = !event.isScrapped
        adapter.notifyDataSetChanged()

        if (event.isScrapped) {
            // [스크랩 추가]
            // 1. scrap_events 컬렉션에 추가
            userRef.collection("scrap_events").document(event.id)
                .set(event) // 이벤트 전체 정보 저장
                .addOnSuccessListener {
                    Log.d("Scrap", "스크랩 성공")

                    // 2. 알림 등록 로직 (endDate가 있는 경우)
                    if (event.endDate != null) {

                        // 행사 마감일 가져오기
                        val calendar = Calendar.getInstance()
                        calendar.time = event.endDate.toDate()

                        // 하루 전으로 설정
                        calendar.add(Calendar.DAY_OF_YEAR, -1)


                        // MainActivity에서 정한 시각으로 설정
                        calendar.set(Calendar.HOUR_OF_DAY, MainActivity.NOTI_HOUR)
                        calendar.set(Calendar.MINUTE, MainActivity.NOTI_MINUTE)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        val notificationTime = Timestamp(calendar.time)

                        val notificationData = hashMapOf(
                            "eventId" to event.id,
                            "title" to event.title,
                            "message" to "${event.title} 마감이 하루 남았습니다!",
                            "timestamp" to notificationTime,
                            "isRead" to false
                        )

                        userRef.collection("notifications").add(notificationData)
                            .addOnSuccessListener { Log.d("Notify", "알림 등록 성공") }
                    }
                }
        } else {
            // [스크랩 취소]
            // 1. scrap_events 컬렉션에서 삭제
            userRef.collection("scrap_events").document(event.id).delete()

            // 2. notifications 컬렉션에서 해당 행사의 알림 삭제
            userRef.collection("notifications")
                .whereEqualTo("eventId", event.id) // eventId가 일치하는 알림 찾기
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        // 찾은 알림 문서 삭제
                        userRef.collection("notifications").document(doc.id).delete()
                            .addOnSuccessListener { Log.d("Notify", "알림 삭제 성공") }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Notify", "알림 삭제 중 오류 발생", e)
                }
        }
    }
}