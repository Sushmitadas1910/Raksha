package com.example.raksha

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class VolumeButtonService : Service() {

    private val channelId = "raksha_volume_sos"
    private val notificationId = 1001

    private var volumePressCount = 0
    private var firstPressTime = 0L
    private val windowMs = 2000L
    private val requiredPresses = 3

    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        volumePressCount = 0
        firstPressTime = 0L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.getStringExtra("action") == "VOLUME_PRESS") {
                handleVolumePress()
            }
        }
        return START_STICKY
    }

    private fun handleVolumePress() {
        val now = System.currentTimeMillis()
        if (volumePressCount == 0) firstPressTime = now
        if (now - firstPressTime > windowMs) {
            volumePressCount = 0
            firstPressTime = now
        }
        volumePressCount++
        handler.removeCallbacks(resetRunnable)
        handler.postDelayed(resetRunnable, windowMs)

        if (volumePressCount >= requiredPresses) {
            volumePressCount = 0
            firstPressTime = 0L
            handler.removeCallbacks(resetRunnable)
            SosManager.trigger(this, silent = true) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Raksha Safety Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Raksha active for volume button SOS"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Raksha is protecting you")
            .setContentText("Press volume down 3× quickly for SOS")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}