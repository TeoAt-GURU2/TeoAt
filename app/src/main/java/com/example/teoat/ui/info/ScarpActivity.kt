package com.example.teoat.ui.info

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.common.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class ScrapActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: EventAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_scrap)
        session = SessionManager(applicationContext)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.rv_scrap_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EventAdapter(emptyList()) { event -> removeScrap(event) }
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        fetchMyScraps()
    }

    // users -> 내ID -> scraps 에 있는 문서 ID들을 가져와서 -> 실제 행사 정보를 찾음
    private fun fetchMyScraps() {
        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. 내 스크랩 목록(ID들) 가져오기
        db.collection("users").document(uid).collection("scraps")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    adapter = EventAdapter(emptyList()) { event -> removeScrap(event) }
                    recyclerView.adapter = adapter
                    Toast.makeText(this, "스크랩한 행사가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 2. 가져온 ID들로 실제 'events' 컬렉션 조회
                // (ID가 10개 미만이면 whereIn을 쓰겠지만, 안전하게 하나씩 가져오거나 전체 비교)
                val scrapIds = documents.map { it.id }
                fetchEventDetails(scrapIds)
            }
    }

    private fun fetchEventDetails(ids: List<String>) {
        // *참고: Firestore whereIn은 최대 10개까지만 가능해서,
        // 편의상 전체 루프나 fieldPath를 씁니다. 여기선 간단하게 구현.

        // 간단한 방법: ID 리스트를 가지고 하나씩 가져와서 합치기
        val eventsList = ArrayList<Event>()
        var loadCount = 0

        for (id in ids) {
            db.collection("events").document(id).get()
                .addOnSuccessListener { doc ->
                    val event = doc.toObject(Event::class.java)
                    if (event != null) {
                        event.id = doc.id
                        event.isScrapped = true // 내 스크랩 화면이니까 당연히 true
                        eventsList.add(event)
                    }
                    loadCount++

                    // 다 가져왔으면 화면 갱신
                    if (loadCount == ids.size) {
                        updateAdapter(eventsList)
                    }
                }
                .addOnFailureListener {
                    loadCount++ // 실패해도 카운트는 올려야 무한대기 안 함
                    if (loadCount == ids.size) updateAdapter(eventsList)
                }
        }
    }

    private fun updateAdapter(list: List<Event>) {
        // 날짜순 정렬 (선택 사항)
        val sortedList = list.sortedBy { it.startDate }

        adapter = EventAdapter(sortedList) { event -> removeScrap(event) }
        recyclerView.adapter = adapter
    }

    private fun removeScrap(event: Event) {
        val uid = session.getUserId() ?: return

        db.collection("users").document(uid).collection("scraps").document(event.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "스크랩 취소됨", Toast.LENGTH_SHORT).show()
                fetchMyScraps() // 목록 새로고침
            }
    }
}