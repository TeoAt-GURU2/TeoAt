package com.example.teoat.ui.info

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.teoat.BuildConfig
import com.example.teoat.R
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityEventBinding
import com.example.teoat.ui.calendar.CalendarApi
import com.example.teoat.ui.calendar.CalendarRepository
import com.example.teoat.ui.calendar.EventDotDecorator
import com.example.teoat.worker.NotiWorker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar

class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding
    private lateinit var adapter: EventAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // 🔥 [핵심] 내 하트 목록을 기억하는 변수
    private val myScrapIds = HashSet<String>()

    private val apiKey: String = BuildConfig.GCAL_API_KEY
    private val calendarId: String = BuildConfig.GCAL_CALENDAR_ID
    private lateinit var repo: CalendarRepository
    private var lastLoadedMonthKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(applicationContext)

        binding.root.findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        // 1. [순서 중요] 내 하트 목록을 먼저 다 가져옵니다.
        loadMyScraps {
            // 2. 다 가져온 뒤에야 화면을 세팅합니다. (그래야 하트가 안 씹힘)
            setupRecyclerView()

            val today = CalendarDay.today()
            loadGoogleDotsForMonth(today.year, today.month)
            fetchFirebaseEvents(today)
        }

        // 구글 캘린더 API 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(CalendarApi::class.java)
        repo = CalendarRepository(api, apiKey, calendarId)

        setupCalendarListener()
    }

    // 내 스크랩(하트) 목록 가져오기
    private fun loadMyScraps(onComplete: () -> Unit) {
        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            onComplete()
            return
        }

        db.collection("users").document(uid).collection("scraps")
            .get()
            .addOnSuccessListener { result ->
                myScrapIds.clear()
                for (document in result) {
                    myScrapIds.add(document.id) // 문서 ID = 행사 ID
                }
                Log.d("EventActivity", "내 스크랩 개수: ${myScrapIds.size}")
                onComplete()
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(emptyList()) { event ->
            val uid = session.getUserId()
            if (uid.isNullOrEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@EventAdapter
            }

            // 1. 화면 즉시 갱신
            event.isScrapped = !event.isScrapped
            adapter.notifyDataSetChanged()

            // 🔥 [핵심] 클릭하자마자 내 기억장치(myScrapIds)에도 바로 반영!
            // 이걸 해야 다른 날짜 갔다 와도 기억함
            if (event.isScrapped) {
                myScrapIds.add(event.id)
            } else {
                myScrapIds.remove(event.id)
            }

            // 2. 파이어베이스 저장은 뒤에서 조용히 처리
            toggleScrapInFirebase(uid, event)
        }
        binding.rvEventList.layoutManager = LinearLayoutManager(this)
        binding.rvEventList.adapter = adapter
    }

    private fun toggleScrapInFirebase(uid: String, event: Event) {
        val scrapRef = db.collection("users").document(uid)
            .collection("scraps").document(event.id)

        if (event.isScrapped) {
            val scrapData = hashMapOf(
                "title" to event.title,
                "date" to event.startDate,
                "savedAt" to Timestamp.now()
            )
            scrapRef.set(scrapData)
                .addOnSuccessListener {
                    // 동료 코드: 알림 등록
                    saveNotificationData(uid, event)
                }
        } else {
            scrapRef.delete()
        }
    }

    private fun saveNotificationData(uid: String, event: Event) {
        val notiData = hashMapOf(
            "title" to event.title,
            "timestamp" to event.startDate,
            "isRead" to false,
            "eventId" to event.id,
            "createdAt" to Timestamp.now()
        )
        db.collection("users").document(uid).collection("notifications")
            .add(notiData)
            .addOnSuccessListener { triggerWorkerImmediate() }
    }

    private fun triggerWorkerImmediate() {
        val workRequest = OneTimeWorkRequestBuilder<NotiWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun setupCalendarListener() {
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            fetchFirebaseEvents(date)
        }
        binding.calendarView.setOnMonthChangedListener { _, date ->
            loadGoogleDotsForMonth(date.year, date.month)
        }
        binding.calendarView.setDateSelected(CalendarDay.today(), true)
    }

    // 🔥 [제일 중요한 함수] 날짜 바꿀 때마다 실행됨
    private fun fetchFirebaseEvents(date: CalendarDay) {
        val startCal = Calendar.getInstance().apply {
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(date.year, date.month - 1, date.day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startTs = Timestamp(startCal.time)
        val endTs = Timestamp(endCal.time)

        db.collection("events")
            .whereGreaterThanOrEqualTo("startDate", startTs)
            .whereLessThanOrEqualTo("startDate", endTs)
            .get()
            .addOnSuccessListener { documents ->
                val events = documents.mapNotNull { doc ->
                    val event = doc.toObject(Event::class.java)
                    event.id = doc.id

                    // 🔥🔥 [여기가 핵심입니다!!!]
                    // 서버에서 가져온 행사가 내 기억장치(myScrapIds)에 있는지 확인해서
                    // 있으면 강제로 하트를 칠해줍니다. 이 코드가 없으면 하트가 계속 사라집니다.
                    if (myScrapIds.contains(event.id)) {
                        event.isScrapped = true
                    } else {
                        event.isScrapped = false
                    }

                    event
                }
                updateList(events)
            }
            .addOnFailureListener { e -> Log.e("EventActivity", "에러", e) }
    }

    private fun loadGoogleDotsForMonth(year: Int, month1Based: Int) {
        val key = "$year-$month1Based"
        if (lastLoadedMonthKey == key) return
        lastLoadedMonthKey = key

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val googleEvents = repo.fetchMonth(year, month1Based - 1)
                val dotDates = HashSet<CalendarDay>()
                googleEvents.forEach { event ->
                    val dateStr = event.start?.date ?: event.start?.dateTime?.take(10)
                    if (dateStr != null) {
                        val parts = dateStr.split("-")
                        if (parts.size >= 3) {
                            val y = parts[0].toInt()
                            val m = parts[1].toInt() - 1
                            val d = parts[2].toInt()
                            dotDates.add(CalendarDay.from(y, m, d))
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.calendarView.removeDecorators()
                    binding.calendarView.addDecorator(EventDotDecorator(dotDates))
                }
            } catch (e: Exception) { Log.e("EventActivity", "API 에러", e) }
        }
    }

    private fun updateList(list: List<Event>) {
        // 어댑터를 새로 만들 때 클릭 리스너도 다시 연결해줍니다.
        adapter = EventAdapter(list) { event ->
            val uid = session.getUserId()
            if(!uid.isNullOrEmpty()) {
                event.isScrapped = !event.isScrapped
                adapter.notifyDataSetChanged()

                // 클릭 시 메모리 업데이트 (중복이지만 안전하게)
                if (event.isScrapped) myScrapIds.add(event.id)
                else myScrapIds.remove(event.id)

                toggleScrapInFirebase(uid, event)
            }
        }
        binding.rvEventList.adapter = adapter
    }
}