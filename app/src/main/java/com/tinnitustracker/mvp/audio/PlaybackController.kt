package com.tinnitustracker.mvp.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.tinnitustracker.mvp.data.ListeningLog
import com.tinnitustracker.mvp.data.Prefs
import com.tinnitustracker.mvp.data.Settings
import com.tinnitustracker.mvp.data.Sound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The one place that plays sound. It owns the engine, audio focus, the headphone-unplug
 * receiver and the foreground service, so the screens, the notification and the system all
 * see the same state. Lives as long as the process; call it from the main thread.
 */
class PlaybackController(
    private val context: Context,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {
    data class State(
        val playing: Boolean = false,
        /** Stopped by a call or another app; starts again when that ends. */
        val interrupted: Boolean = false,
        val sound: Sound = Sound.PINK,
        val notchOn: Boolean = false,
        /** The measured pitch, or null when nothing is measured or there is no clear tone. */
        val notchHz: Float? = null,
    ) {
        val notchActive: Boolean get() = notchOn && notchHz != null
    }

    /** Sleep timer. It counts down only while sound plays. */
    data class Timer(val minutes: Int = 0, val remainingMs: Long = 0) {
        val active: Boolean get() = minutes > 0
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _timer = MutableStateFlow(Timer())
    val timer: StateFlow<Timer> = _timer.asStateFlow()
    private var timerJob: Job? = null
    private var fading = false

    private val _probeStopped = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when the system silences the finder's tone, for example for a call. */
    val probeStopped: SharedFlow<Unit> = _probeStopped.asSharedFlow()

    val engine = SoundEngine(context)
    private val mixer get() = engine.mixer

    /** Kept up to date by [PlaybackService]. */
    var serviceRunning = false

    private val audio = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        // A notification or a navigation prompt only lowers the sound for a moment; the system
        // does that itself. Pausing instead would break the sound for a second or two each time.
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
        .build()
    private var holdsFocus = false

    private val unplugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }
    private var watchingUnplug = false

    fun restore(settings: Settings) {
        _state.value = State(sound = settings.sound, notchOn = settings.notchOn, notchHz = settings.profile?.hz)
        applyNotch()
        engine.prefetch(settings.sound)
    }

    /** Starts the chosen sound. False when another app keeps audio focus. */
    fun play(): Boolean {
        val s = _state.value
        if (s.playing) return true
        if (!requestFocus()) return false
        engine.select(s.sound)
        applyNotch()
        resetFade()
        mixer.therapyOn = true
        engine.wake()
        watchUnplug(true)
        _state.value = s.copy(playing = true, interrupted = false)
        startCounting()
        if (!serviceRunning) PlaybackService.start(context)
        return true
    }

    fun pause() {
        silence(interrupted = false)
        releaseFocusIfIdle()
    }

    /** False when play was refused. */
    fun toggle(): Boolean = if (_state.value.playing) {
        pause()
        true
    } else {
        play()
    }

    fun select(sound: Sound) {
        accrue()   // the time so far belongs to the sound that was playing
        _state.update { it.copy(sound = sound) }
        if (_state.value.playing) engine.select(sound) else engine.prefetch(sound)
        scope.launch { prefs.setSound(sound) }
    }

    fun setNotch(on: Boolean) {
        accrue()
        _state.update { it.copy(notchOn = on) }
        applyNotch()
        scope.launch { prefs.setNotch(on) }
    }

    /** A new measurement: the notch follows the new pitch and turns on when there is one. The caller saves it. */
    fun setMeasuredPitch(hz: Float?) {
        accrue()
        _state.update { it.copy(notchHz = hz, notchOn = hz != null) }
        applyNotch()
    }

    // ── Sleep timer ──────────────────────────────────────────────────────────

    /** Starts, changes or (with 0) cancels the sleep timer. */
    fun setTimer(minutes: Int) {
        timerJob?.cancel()
        if (fading) {
            mixer.fadeTo(1.0, 0.5)   // cancelled mid-fade: come back up gently
            fading = false
        }
        _timer.value = Timer(minutes, minutes * 60_000L)
        if (minutes > 0) timerJob = scope.launch { runTimer() }
    }

    private suspend fun runTimer() {
        var last = SystemClock.elapsedRealtime()
        while (true) {
            delay(TIMER_TICK_MS)
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - last
            last = now
            val t = _timer.value
            if (!t.active) return
            if (!_state.value.playing) continue   // paused: the countdown waits too
            val left = t.remainingMs - elapsed
            if (left <= TIMER_FADE_MS && !fading) {
                fading = true
                mixer.fadeTo(0.0, left.coerceAtLeast(0) / 1000.0)
            }
            if (left <= 0) {
                _timer.value = Timer()
                pause()
                return
            }
            _timer.value = t.copy(remainingMs = left)
        }
    }

    /** Back to full level before sound starts again; safe because nothing is audible yet. */
    private fun resetFade() {
        mixer.fadeTo(1.0, 0.0)
        fading = false
    }

    // ── Listening record ─────────────────────────────────────────────────────
    //
    // A session runs from play to pause. A call or another app only interrupts it; it goes on
    // when the sound comes back. Time is kept per sound and notch setting and written every
    // minute, so a killed app loses at most a minute. Nothing is written until the session has
    // played a minute, so an accidental tap never shows up. The finder's tones never count.

    private var sessionDay: LocalDate? = null
    private var sessionMs = 0L
    /** When the current stretch of playing started (elapsedRealtime), or 0 when not counting. */
    private var countingSince = 0L
    private val unsavedMs = LinkedHashMap<Pair<String, Boolean>, Long>()
    private var recordJob: Job? = null

    private fun startCounting() {
        if (countingSince != 0L) return
        if (sessionDay == null) {
            sessionDay = ListeningLog.sessionDay(LocalDateTime.now())
            sessionMs = 0
        }
        countingSince = SystemClock.elapsedRealtime()
        recordJob?.cancel()
        recordJob = scope.launch {
            while (true) {
                delay(RECORD_EVERY_MS)
                accrue()
                save()
            }
        }
    }

    /** Adds the time since the last call to the sound and notch setting that were playing. */
    private fun accrue() {
        if (countingSince == 0L) return
        val now = SystemClock.elapsedRealtime()
        val ms = now - countingSince
        countingSince = now
        sessionMs += ms
        val s = _state.value
        val key = s.sound.id to s.notchActive
        unsavedMs[key] = (unsavedMs[key] ?: 0L) + ms
    }

    /** Stops counting and saves. [endSession] also closes the session, dropping it if it was under a minute. */
    private fun stopCounting(endSession: Boolean) {
        accrue()
        countingSince = 0L
        recordJob?.cancel()
        recordJob = null
        save()
        if (endSession) {
            sessionDay = null
            sessionMs = 0
            unsavedMs.clear()
        }
    }

    /** Writes the whole seconds not yet saved; the leftover milliseconds wait for the next save. */
    private fun save() {
        val day = sessionDay ?: return
        if (sessionMs < ListeningLog.MIN_SESSION_SECONDS * 1000L) return
        val entries = ArrayList<ListeningLog.Entry>()
        for (e in unsavedMs.entries) {
            val seconds = (e.value / 1000).toInt()
            if (seconds <= 0) continue
            entries += ListeningLog.Entry(day, e.key.first, e.key.second, seconds)
            e.setValue(e.value - seconds * 1000L)
        }
        if (entries.isNotEmpty()) scope.launch { prefs.addListening(entries) }
    }

    // ── Finder probe ─────────────────────────────────────────────────────────

    fun probeTone(hz: Float, level: Float) {
        mixer.probeHz = hz.toDouble()
        mixer.probeLevel = level.toDouble()
        mixer.probe = Mixer.Probe.TONE
        requestFocus()
        engine.wake()
    }

    fun probeHiss(level: Float) {
        mixer.probeLevel = level.toDouble()
        mixer.probe = Mixer.Probe.NOISE
        requestFocus()
        engine.wake()
    }

    fun setProbeHz(hz: Float) {
        mixer.probeHz = hz.toDouble()
    }

    fun setProbeLevel(level: Float) {
        mixer.probeLevel = level.toDouble()
    }

    fun probeOff() {
        if (mixer.probe == Mixer.Probe.OFF) return
        mixer.probe = Mixer.Probe.OFF
        releaseFocusIfIdle()
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private fun silence(interrupted: Boolean) {
        stopCounting(endSession = !interrupted)
        mixer.therapyOn = false
        watchUnplug(false)
        _state.update { it.copy(playing = false, interrupted = interrupted) }
    }

    private fun applyNotch() {
        val s = _state.value
        s.notchHz?.let { mixer.notchHz = it.toDouble() }
        mixer.notchOn = s.notchActive
    }

    private fun requestFocus(): Boolean {
        if (!holdsFocus) {
            holdsFocus = audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return holdsFocus
    }

    private fun releaseFocusIfIdle() {
        val s = _state.value
        if (holdsFocus && !s.playing && !s.interrupted && mixer.probe == Mixer.Probe.OFF) {
            audio.abandonAudioFocusRequest(focusRequest)
            holdsFocus = false
        }
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                holdsFocus = false
                stopProbe()
                silence(interrupted = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                stopProbe()
                if (_state.value.playing) silence(interrupted = true)
            }
            // Normally the system ducks us without calling here. If it does call, keep playing.
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit
            AudioManager.AUDIOFOCUS_GAIN -> if (_state.value.interrupted) {
                resetFade()
                mixer.therapyOn = true
                engine.wake()
                watchUnplug(true)
                _state.update { it.copy(playing = true, interrupted = false) }
                startCounting()
            }
        }
    }

    private fun stopProbe() {
        if (mixer.probe == Mixer.Probe.OFF) return
        mixer.probe = Mixer.Probe.OFF
        _probeStopped.tryEmit(Unit)
    }

    private companion object {
        const val TIMER_TICK_MS = 1_000L
        const val TIMER_FADE_MS = 30_000L
        const val RECORD_EVERY_MS = 60_000L
    }

    private fun watchUnplug(on: Boolean) {
        if (on == watchingUnplug) return
        if (on) {
            ContextCompat.registerReceiver(
                context, unplugReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.unregisterReceiver(unplugReceiver)
        }
        watchingUnplug = on
    }
}
