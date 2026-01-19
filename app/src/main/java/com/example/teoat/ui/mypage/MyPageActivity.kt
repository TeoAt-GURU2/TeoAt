package com.example.teoat.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityMypageLoggedInBinding
import com.example.teoat.databinding.ActivityMypageLoggedOutBinding
import com.example.teoat.ui.auth.JoinActivity

class MyPageActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
    }

    override fun onResume() {
        super.onResume()
        if (session.isLoggedIn()) showLoggedInUi() else showLoggedOutUi()
    }

    private fun showLoggedOutUi() {
        val binding = ActivityMypageLoggedOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val id = binding.etId.text.toString().trim()
            val pw = binding.etPw.text.toString().trim()

            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "아이디/비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 서버 로그인 붙일 자리. 지금은 임시 성공
            session.setLoggedIn(id)
            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
            showLoggedInUi()
        }

        binding.btnGoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }

    private fun showLoggedInUi() {
        val binding = ActivityMypageLoggedInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = session.getUserId() ?: "user"
        binding.tvHello.text = "$userId 님"

        binding.btnEdit.setOnClickListener {
            Toast.makeText(this, "내정보 수정(추후 구현)", Toast.LENGTH_SHORT).show()
        }

        binding.btnInfo.setOnClickListener {
            Toast.makeText(this, "내정보 조회(추후 구현)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            session.logout()
            Toast.makeText(this, "로그아웃", Toast.LENGTH_SHORT).show()
            showLoggedOutUi()
        }
    }
}
