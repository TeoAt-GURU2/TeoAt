package com.example.teoat.ui.chatbot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityChatbotBinding
import com.example.teoat.ui.main.MainActivity
import com.example.teoat.ui.map.FacilityActivity
import com.example.teoat.ui.map.StoreActivity
import com.example.teoat.ui.mypage.MyPageActivity
import kotlinx.coroutines.launch

class ChatbotActivity : BaseActivity() {
    // ViewBinding 객체 선언
    private lateinit var binding : ActivityChatbotBinding

    // ViewModel 연결
    private val viewModel : ChatViewModel by viewModels()

    // Adapter 생성
    private val chatAdapter = ChatAdapter { command ->
        handleNavigationCommand(command)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding 초기화
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UI 초기 설정
        setupRecyclerView()
        setupListeners()

        // ViewModel 관찰 (데이터 변화 감지)
        observeViewModel()

        // 메인에서 넘어온 메세지가 있는지 확인
        handleInitialMessage()
    }

    private fun setupRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatbotActivity).apply {
                stackFromEnd = false // 키보드 올라올 때 리스트가 위로 올라가게 함
            }
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val inputMessage = binding.etMsg.text.toString()
            if (inputMessage.isNotBlank()) {
                // ViewModel로 메세지 전송
                viewModel.sendMessage(inputMessage)
                // 입력창 초기화
                binding.etMsg.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        // Activity 생명 주기에 맞춰 안전하게 수집
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 채팅 데이터 감지 -> Intent 실행
                launch {
                    viewModel.chatUiState.collect { messages ->
                        chatAdapter.submitList(messages) {
                            // 리스트 갱신이 완료되면 스크롤을 맨 아래로 이동
                            if (messages.isNotEmpty()) {
                                binding.rvChat.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleNavigationCommand(command: String) {
        val intent = when (command) {
            "CMD_STORE" -> Intent(this, StoreActivity::class.java)
            "CMD_FACILITY" -> Intent(this, FacilityActivity::class.java)
            "CMD_POLICY" -> Intent(this, MainActivity::class.java)
            "CMD_EVENT" -> Intent(this, MainActivity::class.java)
            "CMD_MYPAGE" -> Intent(this, MyPageActivity::class.java)
            else -> null
        }

        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "이동할 수 없는 메뉴입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleInitialMessage() {
        // "initial_message"라는 키로 전달된 텍스트 불러오기
        val initialQuery = intent.getStringExtra("initial_message")

        // 내용이 있다면 ViewModel로 전송 (전송 버튼 누른 것처럼)
        if (!initialQuery.isNullOrBlank()) {
            viewModel.sendMessage(initialQuery)

            // 화면 회전 시 중복 전송 방지를 위해 Intent 데이터 삭제
            intent.removeExtra("initial_message")
        }
    }
}