package com.example.teoat.ui.calendar

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.teoat.R
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CalendarActivity : AppCompatActivity() {

    private lateinit var todayEventBanner: ViewPager2
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var googleEventRecycler: RecyclerView

    // ↓↓↓ 여기에 너가 적어둔 값 넣으면 됨 (깃에 올리지 마라)
    private val apiKey: String = "AIzaSyDN7HqwCmgn24QhIeeZa7UFb5ctfnKmDuA"
    private val calendarId: String = "0be8deb01781b00a97bace95281bfc4eb7bed1d674606f2ca58899b25c2f687f@group.calendar.google.com"
    // ↑↑↑

    private var allEvents: List<CalendarEventItem> = emptyList()
    private var lastLoadedMonthKey: String? = null

    private lateinit var repo: CalendarRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        todayEventBanner = findViewById(R.id.today_event_banner)
        calendarView = findViewById(R.id.calendarView)
        googleEventRecycler = findViewById(R.id.google_event_recycler)

        googleEventRecycler.layoutManager = LinearLayoutManager(this)

        // Retrofit 세팅
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CalendarApi::class.java)
        repo = CalendarRepository(api, apiKey, calendarId)

        // 날짜 클릭 → 아래 목록 갱신
        calendarView.setOnDateChangedListener { _, date, _ ->
            showEventsForDay(date)
        }

        // 월 이동 → 해당 월 다시 로드 + 점 갱신
        calendarView.setOnMonthChangedListener { _, monthDate ->
            // MaterialCalendarView의 month는 보통 1~12로 들어오는 경우가 많음
            loadGoogleEventsForMonth(monthDate.year, monthDate.month)
        }

        // 초기 로드
        loadCurrentMonth()

        // Firestore 배너 (너 기존 코드 유지)
        loadTodayFirestoreEvents()
    }

    private fun loadCurrentMonth() {
        val cur = calendarView.currentDate ?: CalendarDay.today()
        loadGoogleEventsForMonth(cur.year, cur.month)
    }

    /**
     * month1Based: 1~12 기준으로 받는다.
     * (repo.fetchMonth가 0-based를 원하면 내부에서 -1 해서 호출)
     */
    private fun loadGoogleEventsForMonth(year: Int, month1Based: Int) {
        val key = "%04d-%02d".format(year, month1Based)
        if (lastLoadedMonthKey == key) {
            showEventsForDay(calendarView.selectedDate ?: CalendarDay.today())
            return
        }
        lastLoadedMonthKey = key

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // repo.fetchMonth가 기존에 month0Based를 받는 구조였으니 -1 해서 넘김
                val events = repo.fetchMonth(year, month1Based - 1)
                allEvents = events

                val dotDays = events.mapNotNull { eventToCalendarDay(it) }.toSet()

                withContext(Dispatchers.Main) {
                    calendarView.removeDecorators()
                    calendarView.addDecorator(EventDotDecorator(dotDays))

                    showEventsForDay(calendarView.selectedDate ?: CalendarDay.today())
                }
            } catch (e: Exception) {
                Log.e("CalendarActivity", "Calendar API Error: ${e.message}", e)
            }
        }
    }

    private fun showEventsForDay(day: CalendarDay) {
        val filtered = allEvents.filter { eventToCalendarDay(it) == day }

        // ✅ 여기서 어떤 어댑터를 쓸지 "하나만" 고정해야 함.
        // 너 프로젝트에 GoogleEventAdapter2가 이미 있으니 그걸 계속 쓸게.
        googleEventRecycler.adapter = GoogleEventAdapter2(filtered)
    }

    private fun eventToCalendarDay(e: CalendarEventItem): CalendarDay? {
        val start = e.start ?: return null

        val millis = parseGoogleStartToMillis(
            dateTime = start.dateTime,
            dateOnly = start.date
        ) ?: return null

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        cal.timeInMillis = millis

        val y = cal.get(Calendar.YEAR)
        val m0 = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        return CalendarDay.from(y, m0, d)
    }


    private fun parseGoogleStartToMillis(dateTime: String?, dateOnly: String?): Long? {
        try {
            if (!dateTime.isNullOrBlank()) {
                // 1) milliseconds 없는 형태
                parseWithFormats(
                    dateTime,
                    listOf(
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                    )
                )?.let { return it }
            }

            if (!dateOnly.isNullOrBlank()) {
                // 종일 일정은 로컬(서울) 날짜로 처리
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                return sdf.parse(dateOnly)?.time
            }
        } catch (_: Exception) {
            // ignore
        }
        return null
    }

    private fun parseWithFormats(value: String, patterns: List<String>): Long? {
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                // 'Z' 패턴은 UTC로, 그 외 XXX는 offset 포함이니 그냥 parse해도 됨
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val t = sdf.parse(value)?.time
                if (t != null) return t
            } catch (_: ParseException) {
                // try next
            }
        }
        return null
    }

    // ----------------- 너 기존 Firestore 배너 로직 (그대로) -----------------

    private fun loadTodayFirestoreEvents() {
        val db = FirebaseFirestore.getInstance()

        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val todayStr = "%04d-%02d-%02d".format(y, m, d)

        db.collection("events")
            .whereEqualTo("date", todayStr)
            .get()
            .addOnSuccessListener { documents ->
                val eventList = mutableListOf<TodayEvent>()
                for (document in documents) {
                    val event = document.toObject(TodayEvent::class.java)
                    eventList.add(event)
                }

                if (eventList.isNotEmpty()) {
                    todayEventBanner.adapter = TodayEventAdapter(eventList)
                } else {
                    Log.d("CalendarActivity", "오늘 예정된 행사가 없습니다.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("CalendarActivity", "Firestore 로드 실패: ${e.message}")
            }
    }
}
