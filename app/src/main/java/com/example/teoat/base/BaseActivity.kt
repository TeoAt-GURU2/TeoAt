package com.example.teoat.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.teoat.R
import com.example.teoat.common.SessionManager
import com.example.teoat.data.model.NotificationModel
import com.example.teoat.databinding.ActivityBaseBinding
import com.example.teoat.ui.chatbot.ChatbotActivity
import com.example.teoat.ui.info.EventActivity
import com.example.teoat.ui.main.MainActivity
import com.example.teoat.ui.map.FacilityActivity
import com.example.teoat.ui.map.StoreActivity
import com.example.teoat.ui.mypage.MyPageActivity
import com.example.teoat.ui.notification.NotiAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

open class BaseActivity : AppCompatActivity() {

    // 알림 기능을 위한 변수들
    private lateinit var db : FirebaseFirestore
    private lateinit var session : SessionManager
    private val notiList = mutableListOf<NotificationModel>()
    private lateinit var notiAdapter: NotiAdapter

    // 리스너 관리를 위한 변수
    private var notiListener: ListenerRegistration? = null

    // BaseActivity용 바인딩 객체 선언
    protected lateinit var baseBinding : ActivityBaseBinding

    // 알림 뱃지 뷰 참조를 위한 변수
    private var badgeView: View? = null

    override fun setContentView(layoutResID: Int) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)

        // 자식 액티비티의 레이아웃을 content_frame 안에 넣음
        layoutInflater.inflate(layoutResID, baseBinding.contentFrame, true)

        // 합쳐진 뷰를 화면에 설정
        super.setContentView(baseBinding.root)

        // 툴바 및 네비게이션 초기화
        initCommon()
    }

    override fun setContentView(view: View?) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(view)
        super.setContentView(baseBinding.root)
        initCommon()
    }

    private fun initCommon() {
        db = FirebaseFirestore.getInstance()
        session = SessionManager(this)

        initToolbar()
        initNotificationUI()

        // 읽지 않은 알림들 확인하기 (뱃지 표시용)
        checkUnreadNotifications()
    }

    private fun initToolbar() {
        with (baseBinding.includeToolbar) {
            // 홈 버튼
            ivToolbarHome.setOnClickListener {
                val intent = Intent(this@BaseActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }

            // 메뉴 버튼 : Drawer 열기
            ivToolbarMenu.setOnClickListener {
                if (!baseBinding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    baseBinding.drawerLayout.openDrawer(GravityCompat.END)
                } else {
                    baseBinding.drawerLayout.closeDrawer(GravityCompat.END)
                }
            }

            // 마이페이지 버튼
            ivToolbarMypage.setOnClickListener { startActivity(Intent(this@BaseActivity, MyPageActivity::class.java)) }

            // 뱃지 뷰 찾기
            badgeView = findViewById(R.id.view_noti_badge)

            // 알림창 버튼
            ivToolbarNotice.setOnClickListener {
                // 알림창 열리면 뱃지 안 보이게 수정
                badgeView?.visibility = View.GONE
                openNotificationPanel()
            }
        }
        setUpNavigationDrawer()
    }

    private fun setUpNavigationDrawer() {
        // 네비게이션 드로어 메뉴 헤더 디자인 적용하기
        val menu = baseBinding.navigationView.menu

        setHeaderTitle(menu, R.id.header_facility, "내 주변 시설")
        setHeaderTitle(menu, R.id.header_info, "정보 및 지원")

        // 네비게이션 드로어 메뉴 클릭 리스너
        baseBinding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }
                R.id.nav_mypage -> { startActivity(Intent(this, MyPageActivity::class.java)) }
                R.id.nav_calendar -> { startActivity(Intent(this, MainActivity::class.java)) }
                R.id.nav_card_manage -> {
                    val url = "https://www.gg.go.kr/gdream/view/fma/ordmain/main"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
                R.id.nav_store -> { startActivity(Intent(this, StoreActivity::class.java)) }
                R.id.nav_facility -> { startActivity(Intent(this, FacilityActivity::class.java)) }
                R.id.nav_policy -> { startActivity(Intent(this, MainActivity::class.java)) }
                R.id.nav_event -> { startActivity(Intent(this, EventActivity::class.java)) }
                R.id.nav_chatbot -> { startActivity(Intent(this, ChatbotActivity::class.java)) }
            }
            baseBinding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun initNotificationUI() {
        notiAdapter = NotiAdapter(notiList) { notification ->
            deleteNotification(notification)
        }
        baseBinding.rvNotificationList.layoutManager = LinearLayoutManager(this)
        baseBinding.rvNotificationList.adapter = notiAdapter

        // 닫기 버튼을 누르면 리스너 해제 후 창 닫기
        baseBinding.btnCloseNoti.setOnClickListener { closeNotificationPanel() }

        // 오버레이 바깥쪽 클릭 시에 닫기, 마찬가지로 리스너 해제 후 창 닫기
        baseBinding.flNotificationContainer.setOnClickListener { closeNotificationPanel() }
    }

    // 알림 삭제를 위한 함수
    private fun deleteNotification(notification: NotificationModel) {
        val uid = session.getUserId() ?: return

        // notification.id가 문서 ID라고 가정 (NotificationModel에 id 필드 확인 필요)
        // 만약 id가 비어있다면 문서 로드 시 id를 넣어주는 로직이 필요함
        if (notification.id.isEmpty()) return

        db.collection("users").document(uid)
            .collection("notifications").document(notification.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "알림이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                // 삭제 후 뱃지 상태 재확인
                checkUnreadNotifications()
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // 알림 데이터 불러오기 및 창 열기
    private fun openNotificationPanel() {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 알림창 보이기
        baseBinding.flNotificationContainer.visibility = View.VISIBLE

        val uid = session.getUserId() ?: return

        // 기존 리스너가 있다면 제거
        notiListener?.remove()

        // 실시간 리스너 연결
        notiListener = db.collection("users").document(uid)
            .collection("notifications")
            .whereLessThanOrEqualTo("timestamp", Timestamp.now())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("BaseActivity", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    notiList.clear()
                    for (doc in snapshots) {
                        val noti = doc.toObject(NotificationModel::class.java).copy(id = doc.id)
                        notiList.add(noti)
                    }
                    notiAdapter.notifyDataSetChanged()
                }
            }
    }

    // 알림 창 닫기
    private fun closeNotificationPanel() {
        baseBinding.flNotificationContainer.visibility = View.GONE
        // 패널을 닫으면 실시간 리스너 해제
        notiListener?.remove()
        notiListener = null
    }

    // 알림 뱃지 상태 체크
    private fun checkUnreadNotifications() {
        val uid = session.getUserId() ?: return

        // 단순히 문서가 존재하는지만 체크하여 뱃지 표시
        db.collection("users").document(uid)
            .collection("notifications")
            .whereLessThanOrEqualTo("timestamp", Timestamp.now())
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("BaseActivity", "Badge Listen failed (Index needed?)", e)
                    return@addSnapshotListener
                }

                // 알림창이 열려 있지 않을 때만 뱃지 표시
                if (snapshots != null && !snapshots.isEmpty) {
                    badgeView?.visibility = View.VISIBLE
                } else {
                    badgeView?.visibility = View.GONE
                }
            }

    }

    private fun setHeaderTitle(menu: Menu, itemId: Int,title: String) {
        val item = menu.findItem(itemId)
        val view = item?.actionView

        val tvTitle = view?.findViewById<TextView>(R.id.tv_header_title)
        tvTitle?.text = title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
    }
}