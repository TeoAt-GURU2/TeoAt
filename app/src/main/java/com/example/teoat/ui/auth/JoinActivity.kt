package com.example.teoat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityJoinBinding
import com.example.teoat.ui.main.MainActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        session = SessionManager(this)

        binding.btnJoin.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etId.text.toString().trim() // 아이디 칸 == 이메일
            val pw = binding.etPw.text.toString().trim()
            val pw2 = binding.etPw2.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pw.isEmpty() || pw2.isEmpty() || region.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pw != pw2) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null || age <= 0) {
                Toast.makeText(this, "나이를 올바르게 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createUser(email, pw, name, region, age)
        }

        binding.btnGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun createUser(email: String, pw: String, name: String, region: String, age: Int) {
        auth.createUserWithEmailAndPassword(email, pw)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    Toast.makeText(this, "회원가입 실패(유저 정보 없음)", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uid = user.uid

                val userDoc = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "name" to name,
                    "region" to region,
                    "age" to age,
                    "createdAt" to Timestamp.now()
                )

                db.collection("users").document(uid)
                    .set(userDoc)
                    .addOnSuccessListener {
                        session.setLoggedIn(uid)
                        Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                        goMain()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "DB 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
