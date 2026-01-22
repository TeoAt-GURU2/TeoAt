package com.example.teoat.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityMainBinding
import com.example.teoat.ui.chatbot.ChatbotActivity
import com.example.teoat.ui.main.adapter.BannerAdapter
import com.example.teoat.ui.main.adapter.BannerItem
import com.example.teoat.ui.map.FacilityActivity
import com.example.teoat.ui.map.Store
import com.example.teoat.ui.map.StoreActivity
import com.example.teoat.worker.NotiWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    // 이벤트 배너 슬라이드 자동 실행 여부 제어를 위한 변수
    private var isBannerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이벤트 홍보 배너 설정
        setupBanner()

        // 백그라운드 알림 체크 예약
        setupNotificationWorker()

        // 검색(채팅 시작) 버튼 클릭 시에
        binding.btnStartChat.setOnClickListener {
            val query = binding.etInitialQuery.text.toString().trim()

            if (query.isNotEmpty()) {
                val intent = Intent(this, ChatbotActivity::class.java)
                // 입력한 내용을 intent 에 담기 (Key : "initial_message")
                intent.putExtra("initial_message", query)
                startActivity(intent)

                // 메인화면 입력창 비워주기
                binding.etInitialQuery.text.clear()
            } else {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        // "청소년 정책 찾기" 버튼 클릭 리스너
        binding.btnPolicy.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // "복지 시설 찾기" 버튼 클릭 리스너
        binding.btnFacility.setOnClickListener {
            val intent = Intent(this, FacilityActivity::class.java)
            startActivity(intent)
        }

        // "캘린더 보기" 버튼 클릭 리스너
        binding.btnCalendar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // "급식 카드 가맹점" 버튼 클릭 리스너
        binding.btnStore.setOnClickListener {
            val intent = Intent(this, StoreActivity::class.java)
            startActivity(intent)
        }

        // "급식 카드 관리" 버튼 클릭 리스너
        binding.btnCardManage.setOnClickListener {
            val url = "https://www.gg.go.kr/gdream/view/fma/ordmain/main"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun setupBanner() {
        // 1. 배너 데이터 준비
        val bannerList = listOf(
            BannerItem(R.drawable.banner1),
            BannerItem(R.drawable.banner2)
        )

        // 2. 어댑터 연결
        val bannerAdapter = BannerAdapter(bannerList)
        binding.viewPagerBanner.adapter = bannerAdapter

        // 3. 인디케이터 연결
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPagerBanner) { _, _ ->
            // 텍스트 없이 selector만 적용
        }.attach()

        // 4. 자동 슬라이드 기능 시작
        autoSlideBanner()

        // 5. 사용자가 터치했을 때 슬라이드 멈춤, 재개 처리
        binding.viewPagerBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })
    }

    private fun autoSlideBanner() {
        lifecycleScope.launch {
            while (isBannerRunning) {
                delay(5000)
                val currentItem = binding.viewPagerBanner.currentItem
                val itemCount = binding.viewPagerBanner.adapter?.itemCount ?: 0
                if (itemCount > 0) {
                    val nextItem = if (currentItem < itemCount - 1) currentItem + 1 else 0
                    binding.viewPagerBanner.setCurrentItem(nextItem, true)
                }
            }
        }
    }

    private fun setupNotificationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NotiWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyEventCheck",
            ExistingPeriodicWorkPolicy.KEEP,    // 이미 예약되어 있다면 유지 (중복 실행 방지)
            workRequest
        )
    }

    override fun onResume() {
        super.onResume()
        isBannerRunning = true
        autoSlideBanner()
    }

    override fun onPause() {
        super.onPause()
        isBannerRunning = false
    }
}