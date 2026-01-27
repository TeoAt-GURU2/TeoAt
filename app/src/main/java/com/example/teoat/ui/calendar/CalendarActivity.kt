package com.example.teoat.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.example.teoat.ui.info.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var googleEventRecycler: RecyclerView
    private lateinit var tvNoResult : TextView

    // 구글 캘린더 API 설정
    private val apiKey: String = "AIzaSyDN7HqwCmgn24QhIeeZa7UFb5ctfnKmDuA"
    private val calendarId: String = "0be8deb01781b00a97bace95281bfc4eb7bed1d674606f2ca58899b25c2f687f@group.calendar.google.com"

    // 데이터 저장소
    private var googleEvents: List<CalendarEventItem> = emptyList()
    private var scrapEvents: List<CalendarEventItem> = emptyList()
    private var allEvents: List<CalendarEventItem> = emptyList() // 두 개 합친 것

    private var lastLoadedMonthKey: String? = null
    private lateinit var repo: CalendarRepository
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        initViews()
        initRepository()

        // 1. 데이터 로드 시작
        refreshAllData()
    }

    private fun initViews() {
        // 배너(today_event_banner)는 로직 복잡도를 위해 제거하고, 하단 리스트로 통합함
        calendarView = findViewById(R.id.calendarView)
        googleEventRecycler = findViewById(R.id.google_event_recycler)
        tvNoResult = findViewById(R.id.tv_no_result)

        googleEventRecycler.layoutManager = LinearLayoutManager(this)

        // 날짜 클릭 시 해당 날짜의 일정(구글 + 스크랩) 필터링
        calendarView.setOnDateChangedListener { _, date, _ ->
            showEventsForDay(date)
        }

        // 월 이동 시 구글 캘린더 데이터 다시 로드
        calendarView.setOnMonthChangedListener { _, monthDate ->
            loadGoogleEventsForMonth(monthDate.year, monthDate.month)
        }
    }

    private fun initRepository() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CalendarApi::class.java)
        repo = CalendarRepository(api, apiKey, calendarId)
    }

    private fun refreshAllData() {
        // 1. Firestore 스크랩 데이터 가져오기 (비동기)
        fetchScrapEvents()

        // 2. 구글 캘린더 데이터 가져오기 (현재 월 기준)
        val cur = calendarView.currentDate ?: CalendarDay.today()
        loadGoogleEventsForMonth(cur.year, cur.month)
    }

    // =========================================================
    // 1. Google Calendar 로드 로직
    // =========================================================
    private fun loadGoogleEventsForMonth(year: Int, month1Based: Int) {
        val key = "%04d-%02d".format(year, month1Based)
        // 같은 달을 중복 로드하지 않도록 방지 (필요 시 주석 처리)
        if (lastLoadedMonthKey == key) {
            updateUI() // 데이터는 그대로 두고 UI만 갱신
            return
        }
        lastLoadedMonthKey = key

        lifecycleScope.launch(Dispatchers.IO) {
            val events = repo.fetchMonth(year, month1Based - 1)
            withContext(Dispatchers.Main) {
                googleEvents = events
                updateUI() // 합쳐서 UI 갱신
            }
        }
    }

    // =========================================================
    // 2. Firestore 스크랩 로드 로직 (핵심 수정 부분)
    // =========================================================
    private fun fetchScrapEvents() {
        val user = auth.currentUser
        if (user == null) {
            Log.d("CalendarActivity", "로그인되지 않음 -> 스크랩 로드 건너뜀")
            scrapEvents = emptyList()
            updateUI()
            return
        }

        // 경로 수정: events -> users/{uid}/scrap_events
        db.collection("users").document(user.uid).collection("scrap_events")
            .get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<CalendarEventItem>()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                for (doc in documents) {
                    try {
                        // Event 객체로 변환
                        val event = doc.toObject(Event::class.java)

                        // [중요] Event -> CalendarEventItem 변환
                        // endDate를 캘린더의 '날짜'로 사용
                        val dateStr = event.endDate?.toDate()?.let { sdf.format(it) }

                        if (dateStr != null) {
                            val item = CalendarEventItem(
                                id = event.id,
                                summary = event.title,   // 제목
                                description = event.description,
                                location = event.host,   // 주최 기관을 장소(host) 칸에 표시
                                start = CalendarEventTime(null, dateStr), // 날짜만 있는 종일 일정 취급
                                end = null
                            )
                            list.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e("CalendarActivity", "스크랩 데이터 변환 실패: ${e.message}")
                    }
                }
                scrapEvents = list
                Log.d("CalendarActivity", "스크랩 로드 완료: ${list.size}개")
                updateUI()
            }
            .addOnFailureListener { e ->
                Log.e("CalendarActivity", "스크랩 로드 실패", e)
            }
    }

    // =========================================================
    // 3. UI 통합 갱신 (Dot 찍기 & 리스트 표시)
    // =========================================================
    private fun updateUI() {
        // 두 리스트 병합
        allEvents = googleEvents + scrapEvents

        // 점 찍을 날짜 계산
        val dotDays = allEvents.mapNotNull { CalendarUtils.eventToCalendarDay(it) }.toSet()

        calendarView.removeDecorators()
        calendarView.addDecorator(EventDotDecorator(dotDays))

        // 현재 선택된 날짜의 리스트 갱신
        showEventsForDay(calendarView.selectedDate ?: CalendarDay.today())
    }

    private fun showEventsForDay(day: CalendarDay) {
        val filtered = allEvents.filter { CalendarUtils.eventToCalendarDay(it) == day }
        googleEventRecycler.adapter = GoogleEventAdapter(filtered)

        if (filtered.isEmpty()) {
            tvNoResult.visibility = View.VISIBLE
        } else {
            tvNoResult.visibility = View.GONE
        }
    }
}