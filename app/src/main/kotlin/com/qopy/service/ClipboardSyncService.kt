package com.qopy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.qopy.ui.MainActivity

class ClipboardSyncService : Service() {
    companion object {
        const val CHANNEL_ID = "qopy_sync_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SYNC_NOW = "com.qopy.action.SYNC_NOW"
        const val ACTION_STOP = "com.qopy.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, ClipboardSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Qopy is active", "Silent LAN sync enabled")
        startForeground(NOTIFICATION_ID, notification)

        if (intent?.action == ACTION_SYNC_NOW) {
            handleManualSync()
        }

        return START_STICKY
    }

    private fun handleManualSync() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty()) {
                // Broadcast through network engine
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Qopy Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Qopy LAN clipboard sync active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val syncIntent = Intent(this, ClipboardSyncService::class.java).apply {
            action = ACTION_SYNC_NOW
        }
        val syncPendingIntent = PendingIntent.getService(
            this, 1, syncIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(com.qopy.R.drawable.ic_qopy_notification)
            .setContentIntent(openPendingIntent)
            .addAction(com.qopy.R.drawable.ic_qs_qopy, "Send Clipboard", syncPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
