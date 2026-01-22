package com.example.teoat.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teoat.R
import com.example.teoat.common.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val session = SessionManager(applicationContext)

    override suspend fun doWork(): Result {
        // 1. 로그인 확인. 로그아웃 상태면 작업 중단
        if (!session.isLoggedIn()) { return Result.success() }

        val userId = session.getUserId() ?: return Result.failure()

        try {
            // 2. 사용자가 스크랩한 행사 목록 가져오기
            val scrapSnapshot = db.collection("users").document(userId)
                .collection("scraps")
                .get()
                .await()

            val calendar = Calendar.getInstance()
            // 내일 날짜 구하기 (오늘 + 1일)
            calendar.add(Calendar.DAY_OF_YEAR, 1)

            // 비교를 위한 날짜 형식 통일
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val tomorrowString = sdf.format(calendar.time)

            for (doc in scrapSnapshot.documents) {
                val endDate = doc.getString("endDate") ?: continue
                val title = doc.getString("title") ?: "행사"

                // 3. 마감일이 내일인지?
                if (endDate == tomorrowString) {
                    val message = "'$title' 행사가 내일 진행됩니다!"

                    // 시스템 상단 알림 띄우기
                    showSystemNotification(title, message)

                    // 앱 내 알림 목록에 저장
                    saveNotificationToDb(userId, title, message, doc.id)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val channelId = "event_deadline_channel"
        val notiManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "행사 마감 알림", NotificationManager.IMPORTANCE_DEFAULT)
            notiManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notiManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveNotificationToDb(uid: String, title: String, message: String, eventId: String) {
        val notiData = hashMapOf(
            "title" to title,
            "message" to message,
            "eventId" to eventId,
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )

        // users -> uid -> notifications 컬렉션에 추가
        db.collection("users").document(uid)
            .collection("notifications")
            .add(notiData)
    }
}