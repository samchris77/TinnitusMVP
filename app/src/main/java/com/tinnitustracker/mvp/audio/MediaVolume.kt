package com.tinnitustracker.mvp.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The phone's media volume. The app's volume slider is this, so the hardware keys move it too. */
class MediaVolume(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)

    val max: Int = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val min: Int = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)

    fun current(): Int = audio.getStreamVolume(AudioManager.STREAM_MUSIC)

    fun set(level: Int) {
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(min, max), 0)
        } catch (e: SecurityException) {
            Log.w(TAG, "Volume change refused", e)
        }
    }

    /** Emits the current level, then every change from any source. */
    val levels: Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current())
            }
        }
        ContextCompat.registerReceiver(app, receiver, IntentFilter(VOLUME_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(current())
        awaitClose { app.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private companion object {
        /** Sent by the system on every volume change; not in the public SDK constants. */
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val TAG = "MediaVolume"
    }
}
