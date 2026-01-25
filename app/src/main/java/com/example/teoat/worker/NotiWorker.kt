package com.example.teoat.worker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teoat.R
import com.example.teoat.common.SessionManager
import com.example.teoat.ui.main.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.util.Calendar

class NotiWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val session = SessionManager(context)

    private val CHANNEL_ID = "teoat_channel_v4_urgent"

    override suspend fun doWork(): Result {
        Log.d("NotiWorker", "워커 실행됨!")

        val uid = session.getUserId()
        if (uid.isNullOrEmpty()) return Result.success()

        return try {
            // DB에 저장된 알림들 중 오늘/내일 알려줘야 할 것들 체크
            checkDbAndSendPush(uid)
            Result.success()
        } catch(e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun checkDbAndSendPush(uid: String) {
        // users -> notifications 컬렉션 조회
        val snapshot = db.collection("users").document(uid)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .await()

        // Worker가 실행되는 시점(오늘)
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        for (doc in snapshot.documents) {
            val timestamp = doc.getTimestamp("timestamp") ?: continue
            val eventDate = Calendar.getInstance().apply { time = timestamp.toDate() }
            val title = doc.getString("title") ?: "알림"

            // [날짜 비교 로직]
            // 이벤트 마감일(timestamp)과 내일 날짜가 같은 연/월/일인지 확인 (하루 전 알림)
            if (isSameDay(eventDate, today)) {
                Log.d("NotiWorker", "알림 발송 조건 충족: $title")
                sendSystemNotification(doc.id.hashCode(), title, "마감이 하루 남았습니다!")
            } else {
                Log.d("NotiWorker", "날짜 불일치 (알림 날짜: ${eventDate.time}, 오늘: ${today.time})")
            }
        }
    }

    // 시스템 푸시 알림 발송
    private fun sendSystemNotification(id: Int, title: String, message: String) {

        // 푸시 알림 발송 관련 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 없으면 알림을 보내지 않음
                return
            }
        }
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "일일 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "행사 마감 등 주요 알림을 받습니다."
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // 알림 클릭 시 이동할 Intent
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
        Log.d("NotiWorker", "푸시 알림 발송 완료 ID: $id")
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}