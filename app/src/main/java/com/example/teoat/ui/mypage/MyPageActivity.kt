package com.example.teoat.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityMypageLoggedInBinding
import com.example.teoat.databinding.ActivityMypageLoggedOutBinding
import com.example.teoat.ui.auth.JoinActivity
import com.example.teoat.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onResume() {
        super.onResume()

        // 현재 로그인 상태 확인
        syncSessionWithFirebase()

        // 로그인이 되어 있으면 showLoggedInUi 호출
        if (session.isLoggedIn()) {
            showLoggedInUi()
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()
        }
    }

    private fun syncSessionWithFirebase() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            session.setLoggedIn(firebaseUser.uid)
        } else {
            session.logout()
        }
    }

    // 로그인이 되어 있지 "않은" 상태의 화면
    private fun showLoggedOutUi() {
        val binding = ActivityMypageLoggedOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 버튼 클릭 시 : Login 액티비티로 이동
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 회원가입 버튼 클릭시 : Join 액티비티로 이동
        binding.btnGoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }

    // 로그인된 상태의 화면
    private fun showLoggedInUi() {
        val binding = ActivityMypageLoggedInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            session.logout()
            showLoggedOutUi()
            return
        }

        // 기본 placeholder
        binding.tvHello.text = "안녕하세요"
        binding.tvName.text = "이름: -"
        binding.tvEmail.text = "이메일: -"
        binding.tvRegion.text = "거주지역: -"
        binding.tvAge.text = "나이: -"

        // 자동으로 한번 로드
        loadUserProfile(binding, uid, showToastOnSuccess = false)

        binding.btnInfo.setOnClickListener {
            // "내정보 조회"를 실제로 Firestore에서 다시 가져오게
            loadUserProfile(binding, uid, showToastOnSuccess = true)
        }

        binding.btnEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            // 로그아웃 처리
            auth.signOut()
            session.logout()
            Toast.makeText(this, "로그아웃", Toast.LENGTH_SHORT).show()

            // 바로 로그인 화면 띄우기
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserProfile(
        binding: ActivityMypageLoggedInBinding,
        uid: String,
        showToastOnSuccess: Boolean
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    if (showToastOnSuccess) {
                        Toast.makeText(this, "유저 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }

                val name = doc.getString("name").orEmpty()
                val email = doc.getString("email").orEmpty()
                val region = doc.getString("region").orEmpty()

                // age는 Int로 넣었으니 Long으로 읽히는 경우가 많아서 이렇게 처리
                val ageAny = doc.get("age")
                val age = when (ageAny) {
                    is Long -> ageAny.toInt()
                    is Int -> ageAny
                    else -> null
                }

                // 인사말은 name 우선, 없으면 email
                binding.tvHello.text = when {
                    name.isNotBlank() -> "${name} 님"
                    email.isNotBlank() -> "${email} 님"
                    else -> "안녕하세요"
                }

                binding.tvName.text = "이름: ${if (name.isBlank()) "-" else name}"
                binding.tvEmail.text = "이메일: ${if (email.isBlank()) "-" else email}"
                binding.tvRegion.text = "거주지역: ${if (region.isBlank()) "-" else region}"
                binding.tvAge.text = "나이: ${age ?: "-"}"

                if (showToastOnSuccess) {
                    Toast.makeText(this, "내정보 조회 완료", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (showToastOnSuccess) {
                    Toast.makeText(this, "조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
