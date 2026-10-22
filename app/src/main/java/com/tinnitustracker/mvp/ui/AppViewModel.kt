package com.tinnitustracker.mvp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnitustracker.mvp.MvpApp
import com.tinnitustracker.mvp.audio.PlaybackController
import com.tinnitustracker.mvp.data.CheckIn
import com.tinnitustracker.mvp.data.ListeningLog
import com.tinnitustracker.mvp.data.Profile
import com.tinnitustracker.mvp.data.Sound
import com.tinnitustracker.mvp.util.Pitch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Screen { WELCOME, REFERRAL, PITCH, OCTAVE, LOUDNESS, RESULT, MAIN, FLAGS, MIX, CHECKIN, RECORDS }

/** Sleep-timer choices in minutes; 0 turns it off. */
val TIMER_CHOICES = listOf(0 to "끄기", 15 to "15분", 30 to "30분", 60 to "1시간", 120 to "2시간")

/** What the three finder steps share. */
data class Finder(
    val hz: Float = Pitch.START_HZ,
    val toneOn: Boolean = false,
    val octave: Int = 0,
    val playingOctave: Int? = null,
    val loudness: Int = 0,
    val nonTonal: Boolean = false,
) {
    /** One octave down, the found pitch, one octave up; any outside 250 Hz–16 kHz are left out. */
    val octaveOptions: List<Pair<Int, Float>>
        get() = listOf(-1, 0, 1)
            .map { it to Pitch.round(Pitch.shiftOctaves(hz, it)) }
            .filter { Pitch.inRange(it.second) }
}

/** 내 볼륨 찾기: the media-volume step being tried, and the volume to restore on back. */
data class MixUi(val level: Int = 2, val heard: Boolean = false, val volumeBefore: Int = 0)

/** The weekly check-in in progress: three answers, null until touched. */
data class CheckInUi(
    val step: Int = 0,
    val answers: List<Int?> = listOf(null, null, null),
    val returnTo: Screen = Screen.MAIN,
) {
    val current: Int? get() = answers[step]
}

data class UiState(
    val screen: Screen,
    val profile: Profile?,
    val mixingPoint: Int?,
    val checkIns: List<CheckIn>,
    val finder: Finder = Finder(),
    val mix: MixUi = MixUi(),
    val checkIn: CheckInUi = CheckInUi(),
    val infoOpen: Boolean = false,
    val timerOpen: Boolean = false,
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MvpApp
    private val controller = app.controller

    private val _ui = MutableStateFlow(
        app.settings.value.let {
            UiState(
                screen = if (it.onboarded) Screen.MAIN else Screen.WELCOME,
                profile = it.profile,
                mixingPoint = it.mixingPoint,
                checkIns = it.checkIns,
            )
        },
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    val playback: StateFlow<PlaybackController.State> = controller.state
    val timer: StateFlow<PlaybackController.Timer> = controller.timer
    /** Listening time, saved by the playback controller about once a minute while sound plays. */
    val listening: StateFlow<List<ListeningLog.Entry>> = app.settings
        .map { it.listening }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.settings.value.listening)
    val volumeMax: Int = app.volume.max
    val volume: StateFlow<Int> = app.volume.levels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.volume.current())

    private val finder: Finder get() = _ui.value.finder
    private var octaveJob: Job? = null
    private var messageJob: Job? = null

    init {
        viewModelScope.launch {
            controller.probeStopped.collect { setFinder(finder.copy(toneOn = false, playingOctave = null)) }
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private fun go(screen: Screen, next: Finder = finder) {
        octaveJob?.cancel()
        controller.probeOff()
        _ui.update {
            it.copy(screen = screen, finder = next.copy(toneOn = false, playingOctave = null), infoOpen = false, timerOpen = false)
        }
    }

    private fun setFinder(next: Finder) = _ui.update { it.copy(finder = next) }

    /** Handles back inside the app. False means there is nothing to go back to. */
    fun back(): Boolean {
        val s = _ui.value
        if (s.infoOpen) {
            closeInfo()
            return true
        }
        if (s.timerOpen) {
            closeTimer()
            return true
        }
        val target = when (s.screen) {
            Screen.REFERRAL -> Screen.WELCOME
            Screen.PITCH -> Screen.MAIN
            Screen.OCTAVE -> Screen.PITCH
            Screen.LOUDNESS -> if (s.finder.nonTonal) Screen.PITCH else Screen.OCTAVE
            Screen.RESULT -> Screen.LOUDNESS
            Screen.FLAGS -> Screen.MAIN
            Screen.MIX -> {
                app.volume.set(s.mix.volumeBefore)   // cancelled: put the volume back
                Screen.MAIN
            }
            Screen.CHECKIN -> {
                if (s.checkIn.step > 0) {
                    _ui.update { it.copy(checkIn = it.checkIn.copy(step = it.checkIn.step - 1)) }
                    return true
                }
                s.checkIn.returnTo
            }
            Screen.RECORDS -> Screen.MAIN
            Screen.WELCOME, Screen.MAIN -> return false
        }
        go(target)
        return true
    }

    /** Leaving the app silences the finder's tone; the chosen sound keeps playing. */
    fun onBackground() {
        octaveJob?.cancel()
        controller.probeOff()
        setFinder(finder.copy(toneOn = false, playingOctave = null))
    }

    // ── First launch ─────────────────────────────────────────────────────────

    fun answerSafety(anyApply: Boolean) {
        if (anyApply) go(Screen.REFERRAL) else startFirstFinder()
    }

    fun continueAfterReferral() = startFirstFinder()

    private fun startFirstFinder() {
        viewModelScope.launch { app.prefs.setOnboarded() }
        go(Screen.PITCH, Finder())
    }

    // ── Finder ───────────────────────────────────────────────────────────────

    fun toggleTone() {
        val f = finder
        if (f.toneOn) {
            controller.probeOff()
            setFinder(f.copy(toneOn = false))
        } else {
            startProbe(f)
        }
    }

    private fun startProbe(f: Finder) {
        val loudnessStep = _ui.value.screen == Screen.LOUDNESS
        when {
            loudnessStep && f.nonTonal -> controller.probeHiss(Pitch.loudnessGain(f.loudness))
            loudnessStep -> controller.probeTone(f.hz, Pitch.loudnessGain(f.loudness))
            else -> controller.probeTone(f.hz, Pitch.TONE_LEVEL)
        }
        setFinder(f.copy(toneOn = true))
    }

    /** Touching the pitch slider or its buttons starts the tone. */
    fun setPitch(fraction: Float) = updatePitch(Pitch.clampRound(Pitch.fromFraction(fraction)))

    fun stepPitch(direction: Int) = updatePitch(Pitch.clampRound(Pitch.shiftSemitones(finder.hz, direction)))

    private fun updatePitch(hz: Float) {
        val f = finder.copy(hz = hz)
        if (f.toneOn) {
            controller.setProbeHz(hz)
            setFinder(f)
        } else {
            startProbe(f)
        }
    }

    fun pitchDone() = go(Screen.OCTAVE, finder.copy(octave = 0, nonTonal = false))

    fun noClearTone() = go(Screen.LOUDNESS, finder.copy(nonTonal = true, loudness = 0))

    /** Plays one octave option for two seconds and selects it. */
    fun tryOctave(octave: Int) {
        val f = finder
        controller.probeTone(Pitch.round(Pitch.shiftOctaves(f.hz, octave)), Pitch.TONE_LEVEL)
        setFinder(f.copy(octave = octave, playingOctave = octave))
        octaveJob?.cancel()
        octaveJob = viewModelScope.launch {
            delay(2_000)
            controller.probeOff()
            setFinder(finder.copy(playingOctave = null))
        }
    }

    fun octaveDone() {
        val f = finder
        go(Screen.LOUDNESS, f.copy(hz = Pitch.clampRound(Pitch.shiftOctaves(f.hz, f.octave)), octave = 0, loudness = 0))
    }

    /** Touching the loudness slider or its buttons starts the sound, from silence. */
    fun setLoudness(value: Int) {
        val f = finder.copy(loudness = value.coerceIn(0, 100))
        if (f.toneOn) {
            controller.setProbeLevel(Pitch.loudnessGain(f.loudness))
            setFinder(f)
        } else {
            startProbe(f)
        }
    }

    fun stepLoudness(direction: Int) = setLoudness(finder.loudness + direction * 2)

    fun loudnessDone() = go(Screen.RESULT)

    fun redo() = go(Screen.PITCH, finder.copy(nonTonal = false, loudness = 0))

    fun save() {
        val f = finder
        val profile = Profile(hz = if (f.nonTonal) null else f.hz, loudness = f.loudness, measuredAt = System.currentTimeMillis())
        controller.setMeasuredPitch(profile.hz)
        viewModelScope.launch { app.prefs.saveProfile(profile, notchOn = profile.hz != null) }
        _ui.update { it.copy(profile = profile) }
        go(Screen.MAIN)
        showMessage("내 이명을 저장했어요")
    }

    // ── Main screen ──────────────────────────────────────────────────────────

    fun findAgain() {
        if (controller.state.value.playing) controller.pause()
        go(Screen.PITCH, Finder(hz = _ui.value.profile?.hz ?: Pitch.START_HZ))
    }

    fun selectSound(sound: Sound) = controller.select(sound)

    fun toggleNotch() = controller.setNotch(!controller.state.value.notchOn)

    fun togglePlay() {
        if (!controller.toggle()) showMessage("다른 앱이 소리를 쓰고 있어요")
        if (controller.state.value.playing) _ui.update { it.copy(mix = it.mix.copy(heard = true)) }
    }

    fun setVolume(level: Int) = app.volume.set(level)

    fun openInfo() = _ui.update { it.copy(infoOpen = true) }

    fun closeInfo() = _ui.update { it.copy(infoOpen = false) }

    fun openFlags() = go(Screen.FLAGS)

    // ── 내 볼륨 찾기 ──────────────────────────────────────────────────────────

    /** Starts low, so nothing is loud when the screen opens. */
    fun openMix() {
        val before = app.volume.current()
        val start = minOf(before, MIX_START_LEVEL)
        app.volume.set(start)
        go(Screen.MIX)
        _ui.update { it.copy(mix = MixUi(level = start, heard = controller.state.value.playing, volumeBefore = before)) }
    }

    /** Touching the slider or the step buttons starts the sound. */
    fun setMixLevel(level: Int) {
        val l = level.coerceIn(0, volumeMax)
        app.volume.set(l)
        if (l > 0 && !controller.state.value.playing) controller.play()
        val playing = controller.state.value.playing
        _ui.update { it.copy(mix = it.mix.copy(level = l, heard = it.mix.heard || playing)) }
    }

    fun stepMixLevel(direction: Int) = setMixLevel(_ui.value.mix.level + direction)

    /** Saves the mixing point and sets the volume one step below it. */
    fun saveMix() {
        val point = _ui.value.mix.level
        val recommended = maxOf(1, point - 1)
        viewModelScope.launch { app.prefs.saveMixingPoint(point) }
        app.volume.set(recommended)
        _ui.update { it.copy(mixingPoint = point) }
        go(Screen.MAIN)
        showMessage("권장 볼륨으로 맞췄어요 · $recommended / $volumeMax")
    }

    // ── Weekly check-in ──────────────────────────────────────────────────────

    fun openCheckIn() {
        val from = _ui.value.screen
        go(Screen.CHECKIN)
        _ui.update { it.copy(checkIn = CheckInUi(returnTo = from)) }
    }

    fun answerCheckIn(value: Int) = _ui.update {
        val c = it.checkIn
        it.copy(checkIn = c.copy(answers = c.answers.toMutableList().also { a -> a[c.step] = value }))
    }

    fun checkInNext() {
        val c = _ui.value.checkIn
        if (c.current == null) return
        if (c.step < 2) {
            _ui.update { it.copy(checkIn = c.copy(step = c.step + 1)) }
            return
        }
        val entry = CheckIn(LocalDate.now(), c.answers[0]!!, c.answers[1]!!, c.answers[2]!!)
        viewModelScope.launch { app.prefs.addCheckIn(entry) }
        _ui.update { it.copy(checkIns = CheckIn.add(it.checkIns, entry)) }
        go(Screen.RECORDS)
        showMessage("이번 주 기록했어요")
    }

    fun openRecords() = go(Screen.RECORDS)

    // ── Sleep timer ──────────────────────────────────────────────────────────

    fun openTimer() = _ui.update { it.copy(timerOpen = true) }

    fun closeTimer() = _ui.update { it.copy(timerOpen = false) }

    fun setTimer(minutes: Int) {
        controller.setTimer(minutes)
        _ui.update { it.copy(timerOpen = false) }
        val label = TIMER_CHOICES.firstOrNull { it.first == minutes }?.second ?: "${minutes}분"
        showMessage(
            when {
                minutes == 0 -> "타이머를 껐어요"
                controller.state.value.playing -> "$label 뒤에 꺼져요"
                else -> "재생하면 $label 타이머가 시작돼요"
            },
        )
    }

    private fun showMessage(text: String) {
        messageJob?.cancel()
        _ui.update { it.copy(message = text) }
        messageJob = viewModelScope.launch {
            delay(2_400)
            _ui.update { it.copy(message = null) }
        }
    }

    private companion object {
        const val MIX_START_LEVEL = 2
    }
}
