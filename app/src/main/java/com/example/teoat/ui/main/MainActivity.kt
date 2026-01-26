package com.example.teoat.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityMainBinding
import com.example.teoat.ui.chatbot.ChatbotActivity
import com.example.teoat.ui.info.EventActivity
import com.example.teoat.ui.info.ScrapActivity // 👈 [추가됨] 스크랩 화면 Import
import com.example.teoat.ui.main.adapter.BannerAdapter
import com.example.teoat.ui.main.adapter.BannerItem
import com.example.teoat.ui.map.FacilityActivity
import com.example.teoat.ui.map.StoreActivity
import com.example.teoat.worker.NotiWorker
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    // 이벤트 배너 슬라이드 자동 실행 여부 제어를 위한 변수
    private var isBannerRunning = false

    companion object {
        const val NOTI_HOUR = 23
        const val NOTI_MINUTE = 16
    }

    // 알림 권한 요청 런처
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알림 권한이 거부되어 알림을 받을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 알림 권한 확인 및 요청
        checkNotificationPermission()

        // 이벤트 홍보 배너 설정
        setupBanner()

        // 백그라운드 알림 체크 예약
        setupNotificationWorker()

        // 버튼 리스너 분리
        setUpButtons()
    }

    // 알림 권한 확인 함수
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            // 권한이 없으면 사용자에게 팝업으로 요청
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 버튼 리스너 모아둔 함수
    private fun setUpButtons() {
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
            val intent = Intent(this, EventActivity::class.java)
            startActivity(intent)
        }

        // "복지 시설 찾기" 버튼 클릭 리스너
        binding.btnFacility.setOnClickListener {
            val intent = Intent(this, FacilityActivity::class.java)
            startActivity(intent)
        }

        // "스크랩한 행사" (ID가 btnCalendar인 버튼) 클릭 리스너
        // 👇 [수정됨] MainActivity -> ScrapActivity 로 변경!
        binding.btnCalendar.setOnClickListener {
            val intent = Intent(this, ScrapActivity::class.java)
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

        // 현재 시각과 목표 시각의 차이(delay) 계산
        val delay = calculateInitialDelay(NOTI_HOUR, NOTI_MINUTE)

        // WorkManager요청 생성
        val workRequest = PeriodicWorkRequestBuilder<NotiWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("daily_noti")
            .build()

        // 작업 예약
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyEventCheck",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        // 안내용 토스트 (선택 사항)
        // Toast.makeText(this, "매일 $NOTI_HOUR:$NOTI_MINUTE 에 알림을 확인합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()

        targetTime.set(Calendar.HOUR_OF_DAY, targetHour)
        targetTime.set(Calendar.MINUTE, targetMinute)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        // 만약 목표 시간이 지났다면 내일로 설정
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
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