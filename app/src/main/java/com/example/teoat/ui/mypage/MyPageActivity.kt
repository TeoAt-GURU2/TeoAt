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

    /**
     * FirebaseAuth 상태랑 SessionManager 상태가 어긋날 수 있어서 한번 맞춰줌
     * - Firebase에 유저가 있으면 SessionManager에도 uid 저장
     * - Firebase에 유저가 없으면 SessionManager도 로그아웃 처리
     */
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

        // "로그인" 버튼 -> LoginActivity로 이동
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // "회원가입" 버튼 -> JoinActivity로 이동
        binding.btnGoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }

    private fun showLoggedInUi() {
        val binding = ActivityMypageLoggedInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            // 세션이 비정상인 경우
            session.logout()
            showLoggedOutUi()
            return
        }

        // 기본은 uid 보여주고, Firestore에 email 있으면 가져와서 표시
        binding.tvHello.text = "${uid} 님"

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val email = doc.getString("email")
                if (!email.isNullOrEmpty()) {
                    binding.tvHello.text = "${email} 님"
                }
            }
            .addOnFailureListener {
                // 실패해도 앱이 죽을 필요 없으니 조용히 넘어감
            }

        binding.btnEdit.setOnClickListener {
            Toast.makeText(this, "내정보 수정(추후 구현)", Toast.LENGTH_SHORT).show()
        }

        binding.btnInfo.setOnClickListener {
            Toast.makeText(this, "내정보 조회(추후 구현)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            session.logout()
            Toast.makeText(this, "로그아웃", Toast.LENGTH_SHORT).show()
            showLoggedOutUi()
        }
    }
}

