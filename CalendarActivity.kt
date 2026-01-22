package com.example.teoat.ui.Calendar

import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.teoat.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var todayEventBanner: ViewPager2
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var googleEventRecycler: RecyclerView

    private var allEvents: List<Event> = listOf()

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) loadGoogleEvents()
    }

    private val authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) loadGoogleEvents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 1. 뷰 초기화
        todayEventBanner = findViewById(R.id.today_event_banner)
        calendarView = findViewById(R.id.calendarView)
        googleEventRecycler = findViewById(R.id.google_event_recycler)
        googleEventRecycler.layoutManager = LinearLayoutManager(this)

        // 2. 구글 로그인 및 권한 체크
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account == null || !GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR_READONLY))) {
            signInLauncher.launch(client.signInIntent)
        } else {
            loadGoogleEvents()
        }

        // 3. 달력 날짜 클릭 이벤트
        calendarView.setOnDateChangedListener { _, date, _ ->
            filterEventsByDate(date.year, date.month, date.day)
        }

        // 4. Firestore에서 오늘 행사 불러오기 (배너용)
        loadTodayFirestoreEvents()
    }

    // [추가된 기능] Firestore에서 오늘 날짜의 행사를 가져와 배너에 연결
    private fun loadTodayFirestoreEvents() {
        val db = FirebaseFirestore.getInstance()

        // Firestore에 저장된 날짜 형식(yyyy-MM-dd)과 일치시킴
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        db.collection("events")
            .whereEqualTo("date", todayStr)
            .get()
            .addOnSuccessListener { documents ->
                val eventList = mutableListOf<TodayEvent>() // 모델 클래스 확인 필요
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

    private fun loadGoogleEvents() {
        val account = GoogleSignIn.getLastSignedInAccount(this) ?: return
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(CalendarScopes.CALENDAR_READONLY)
        ).setSelectedAccountName(account.email)

        val service = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName("TeoAt").build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 현재 시간 기준 일정 가져오기
                val now = com.google.api.client.util.DateTime(System.currentTimeMillis())
                val googleEvents = service.events().list("primary")
                    .setTimeMin(now)
                    .setSingleEvents(true)
                    .execute()

                allEvents = googleEvents.items ?: listOf()

                withContext(Dispatchers.Main) {
                    googleEventRecycler.adapter = GoogleEventAdapter(allEvents)
                }
            } catch (e: UserRecoverableAuthIOException) {
                withContext(Dispatchers.Main) { authLauncher.launch(e.intent) }
            } catch (e: Exception) {
                Log.e("CalendarActivity", "Google API Error: ${e.message}")
            }
        }
    }

    private fun filterEventsByDate(year: Int, month: Int, day: Int) {
        // month는 1부터 시작 (MaterialCalendarView 기준)
        val selectedDateStr = String.format("%04d-%02d-%02d", year, month, day)
        val filteredList = allEvents.filter {
            val start = it.start.dateTime ?: it.start.date
            start.toString().contains(selectedDateStr)
        }
        googleEventRecycler.adapter = GoogleEventAdapter(filteredList)
    }
}