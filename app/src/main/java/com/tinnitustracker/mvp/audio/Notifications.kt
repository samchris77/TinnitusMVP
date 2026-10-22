package com.tinnitustracker.mvp.audio

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tinnitustracker.mvp.MainActivity
import com.tinnitustracker.mvp.R
import com.tinnitustracker.mvp.util.Pitch

/** The playback notification: the sound's name, the notch state, pause or play, and close. */
object Notifications {
    const val ID = 1001
    private const val CHANNEL = "playback"

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("재생")
            .setDescription("소리가 나오는 동안 표시돼요")
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** [minutesLeft] is the sleep timer, shown only while sound plays. */
    fun build(context: Context, s: PlaybackController.State, minutesLeft: Int? = null): Notification {
        val timer = minutesLeft?.let { "${it}분 뒤 꺼짐" }
        val text = when {
            s.interrupted -> "잠시 멈춤"
            !s.playing -> "일시정지됨"
            timer != null && s.notchActive -> "$timer · 이명 음 빼기 켜짐"
            timer != null -> timer
            s.notchActive -> "이명 음 빼기 켜짐 · ${Pitch.format(s.notchHz!!)}"
            else -> "재생 중"
        }
        val toggle = if (s.playing) {
            NotificationCompat.Action(R.drawable.ic_pause, "일시정지", serviceIntent(context, PlaybackService.ACTION_PAUSE))
        } else {
            NotificationCompat.Action(R.drawable.ic_play, "재생", foregroundIntent(context, PlaybackService.ACTION_PLAY))
        }
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.sound.fullName)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setOngoing(s.playing || s.interrupted)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(toggle)
            .addAction(NotificationCompat.Action(R.drawable.ic_close, "끄기", serviceIntent(context, PlaybackService.ACTION_CLOSE)))
            .build()
    }

    /** Posts or updates the notification outside the foreground service, if allowed. */
    fun post(context: Context, s: PlaybackController.State, minutesLeft: Int? = null) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(ID, build(context, s, minutesLeft))
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }

    private fun serviceIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
        context, action.hashCode(),
        Intent(context, PlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun foregroundIntent(context: Context, action: String): PendingIntent = PendingIntent.getForegroundService(
        context, action.hashCode(),
        Intent(context, PlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
