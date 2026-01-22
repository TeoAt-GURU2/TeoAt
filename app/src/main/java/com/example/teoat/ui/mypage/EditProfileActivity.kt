package com.example.teoat.ui.mypage

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teoat.common.SessionManager
import com.example.teoat.databinding.ActivityEditProfileBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var session: SessionManager
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FirebaseFirestore.getInstance()

        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "로그인이 필요함", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 기존 정보 로드
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.etName.setText(doc.getString("name") ?: "")
                binding.etRegion.setText(doc.getString("region") ?: "")
                val age = doc.getLong("age")?.toInt()
                binding.etAge.setText(age?.toString() ?: "")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "정보 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // 저장
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()

            if (name.isEmpty() || region.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(this, "모든 항목 입력", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null || age <= 0) {
                Toast.makeText(this, "나이 올바르게 입력", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val update = mapOf(
                "name" to name,
                "region" to region,
                "age" to age
            )

            db.collection("users").document(uid).update(update)
                .addOnSuccessListener {
                    Toast.makeText(this, "수정 완료", Toast.LENGTH_SHORT).show()
                    finish()
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
