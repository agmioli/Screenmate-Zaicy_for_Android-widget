package com.example.personaz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FloatingPersonazService : Service() {

    private lateinit var windowManager: WindowManager
    private var personazView: PersonazView? = null

    companion object {
        var instance: FloatingPersonazService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        personazView = PersonazView(this, windowManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "personaz_channel",
                "Персонаж",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление для работы персонажа поверх окон"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        personazView?.destroy()
        personazView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, "personaz_channel")
            .setContentTitle("Персонаж активен")
            .setContentText("Нажмите для открытия приложения")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun updatePersonazSettings(size: Int, rotation: Float, scaleX: Float, scaleY: Float) {
        personazView?.updateAll(size, rotation, scaleX, scaleY)
    }

    fun hidePersonaz() {
        personazView?.destroy()
        personazView = null
    }

    fun showPersonaz() {
        if (personazView == null) {
            personazView = PersonazView(this, windowManager)
        }
    }
}