package com.example.teoat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityLoginBinding
import com.example.teoat.ui.main.MainActivity
import com.example.teoat.ui.mypage.MyPageActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        session = SessionManager(this)

        binding.btnLogin.setOnClickListener {
            val email = binding.etId.text.toString().trim() // 아이디 칸 == 이메일
            val pw = binding.etPw.text.toString().trim()

            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "아이디(이메일)와 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pw)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid == null) {
                        Toast.makeText(this, "로그인 실패(유저 정보 없음)", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    session.setLoggedIn(uid)
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MyPageActivity::class.java)
                    startActivity(intent)

                    // 기존 스택을 정리하고 새로 시작
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)

                    finish() // 로그인 창 닫기
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnGoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }
}
