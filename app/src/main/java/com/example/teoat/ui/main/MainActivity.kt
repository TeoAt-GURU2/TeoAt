package com.example.teoat.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.R
import com.example.teoat.ui.chatbot.ChatbotActivity
import com.example.teoat.ui.mypage.MyPageActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<LinearLayout>(R.id.btnAccount).setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        findViewById<Button>(R.id.btnChatbot).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        findViewById<Button>(R.id.btnPolicy).setOnClickListener {
            Toast.makeText(this, "정책 찾기(추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnFacility).setOnClickListener {
            Toast.makeText(this, "복지시설 찾기(추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnCalendar).setOnClickListener {
            Toast.makeText(this, "캘린더(추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnStore).setOnClickListener {
            Toast.makeText(this, "가맹점 찾기(추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnCardManage).setOnClickListener {
            Toast.makeText(this, "급식카드 관리(추후 연결)", Toast.LENGTH_SHORT).show()
        }
    }
}
