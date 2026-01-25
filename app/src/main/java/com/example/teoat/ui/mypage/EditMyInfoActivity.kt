package com.example.teoat.ui.mypage

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityEditMyinfoBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditMyInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditMyinfoBinding
    private lateinit var session: SessionManager
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditMyinfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FirebaseFirestore.getInstance()

        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 기존 값 불러와서 기본 세팅
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                binding.etName.setText(doc.getString("name") ?: "")
                binding.etRegion.setText(doc.getString("region") ?: "")
                val ageAny = doc.get("age")
                val age = when (ageAny) {
                    is Long -> ageAny.toInt()
                    is Int -> ageAny
                    else -> null
                }
                binding.etAge.setText(age?.toString() ?: "")
            }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()

            if (name.isEmpty() || region.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null || age <= 0) {
                Toast.makeText(this, "나이를 올바르게 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any>(
                "name" to name,
                "region" to region,
                "age" to age
            )

            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "수정 완료", Toast.LENGTH_SHORT).show()
                    finish() // 돌아가면 MyPage onResume에서 다시 조회됨
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
