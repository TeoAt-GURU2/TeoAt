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
        syncSessionWithFirebase()
        if (session.isLoggedIn()) showLoggedInUi() else showLoggedOutUi()
    }

    private fun syncSessionWithFirebase() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            session.setLoggedIn(firebaseUser.uid)
        } else {
            session.logout()
        }
    }

    private fun showLoggedOutUi() {
        val binding = ActivityMypageLoggedOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnGoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }

    }

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
            auth.signOut()
            session.logout()
            Toast.makeText(this, "로그아웃", Toast.LENGTH_SHORT).show()
            showLoggedOutUi()
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
