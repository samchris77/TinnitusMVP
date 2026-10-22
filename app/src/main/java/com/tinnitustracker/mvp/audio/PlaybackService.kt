package com.tinnitustracker.mvp.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.tinnitustracker.mvp.MvpApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Keeps sound going with the screen off. Exists only while a sound plays (or is interrupted by
 * a call); on pause it leaves the foreground, leaves a notification that can resume, and stops.
 */
class PlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller get() = (application as MvpApp).controller
    private var wakeLock: PowerManager.WakeLock? = null
    private var leaving = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        controller.serviceRunning = true
        Notifications.createChannel(this)
        scope.launch {
            // Re-post only when something visible changes; the timer moves once a minute here.
            combine(controller.state, controller.timer) { s, t -> shown(s, t) }
                .distinctUntilChanged()
                .drop(1)
                .collect { shown ->
                    if (leaving) return@collect   // a new instance takes over if play starts again
                    if (shown.state.playing || shown.state.interrupted) goForeground(shown) else settle(shown)
                }
        }
    }

    private data class Shown(val state: PlaybackController.State, val minutesLeft: Int?)

    private fun shown(s: PlaybackController.State, t: PlaybackController.Timer) =
        Shown(s, if (t.active && s.playing) ceil(t.remainingMs / 60_000.0).toInt() else null)

    private fun current() = shown(controller.state.value, controller.timer.value)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            controller.pause()
            Notifications.cancel(this)
            leave()
            return START_NOT_STICKY
        }
        goForeground(current())   // started with startForegroundService: must go foreground now
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_PLAY -> if (!controller.play()) settle(current())
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        controller.serviceRunning = false
        super.onDestroy()
    }

    private fun goForeground(shown: Shown) {
        ServiceCompat.startForeground(
            this, Notifications.ID, Notifications.build(this, shown.state, shown.minutesLeft),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (shown.state.playing) holdWakeLock() else releaseWakeLock()
    }

    /** Paused by the user: leave a notification that can resume, then stop. */
    private fun settle(shown: Shown) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        Notifications.post(this, shown.state, shown.minutesLeft)
        leave()
    }

    private fun leave() {
        leaving = true
        releaseWakeLock()
        controller.serviceRunning = false
        stopSelf()
    }

    private fun holdWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TinnitusMVP:playback")
            .apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        const val ACTION_PLAY = "com.tinnitustracker.mvp.PLAY"
        const val ACTION_PAUSE = "com.tinnitustracker.mvp.PAUSE"
        const val ACTION_CLOSE = "com.tinnitustracker.mvp.CLOSE"
        private const val WAKE_LOCK_TIMEOUT_MS = 12 * 60 * 60 * 1000L
        private const val TAG = "PlaybackService"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12+ refuses some background starts; the sound still plays while the app is open.
                Log.w(TAG, "Could not start playback service", e)
            }
        }
    }
}
