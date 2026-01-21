package com.example.teoat.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.teoat.R
import com.example.teoat.databinding.ActivityBaseBinding
import com.example.teoat.ui.chatbot.ChatbotActivity
import com.example.teoat.ui.main.MainActivity
import com.example.teoat.ui.map.FacilityActivity
import com.example.teoat.ui.map.StoreActivity
import com.example.teoat.ui.mypage.MyPageActivity
import com.google.firebase.ai.type.content

open class BaseActivity : AppCompatActivity() {

    // BaseActivity용 바인딩 객체 선언
    protected lateinit var baseBinding : ActivityBaseBinding

    override fun setContentView(layoutResID: Int) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)

        // 자식 액티비티의 레이아웃을 content_frame 안에 넣음
        layoutInflater.inflate(layoutResID, baseBinding.contentFrame, true)

        // 합쳐진 뷰를 화면에 설정
        super.setContentView(baseBinding.root)

        // 툴바 및 네비게이션 초기화
        initToolbar()
    }

    override fun setContentView(view: View?) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(view)
        super.setContentView(baseBinding.root)
        initToolbar()
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

            // 알림창 버튼
            ivToolbarNotice.setOnClickListener { startActivity(Intent(this@BaseActivity, MainActivity::class.java)) }
        }

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
                R.id.nav_event -> { startActivity(Intent(this, MainActivity::class.java)) }
                R.id.nav_chatbot -> { startActivity(Intent(this, ChatbotActivity::class.java)) }
            }
            baseBinding.drawerLayout.closeDrawer(GravityCompat.END)
            true
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