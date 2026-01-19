package com.example.teoat.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.teoat.R
import com.example.teoat.base.BaseActivity
import com.example.teoat.databinding.ActivityMainBinding
import com.example.teoat.ui.chatbot.ChatbotActivity

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    }
}