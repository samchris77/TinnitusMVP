package com.tinnitustracker.mvp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinnitustracker.mvp.ui.screens.CheckInScreen
import com.tinnitustracker.mvp.ui.screens.FlagsScreen
import com.tinnitustracker.mvp.ui.screens.MixScreen
import com.tinnitustracker.mvp.ui.screens.RecordsScreen
import com.tinnitustracker.mvp.ui.screens.TimerSheet
import com.tinnitustracker.mvp.ui.screens.InfoSheet
import com.tinnitustracker.mvp.ui.screens.LoudnessScreen
import com.tinnitustracker.mvp.ui.screens.MainScreen
import com.tinnitustracker.mvp.ui.screens.OctaveScreen
import com.tinnitustracker.mvp.ui.screens.PitchScreen
import com.tinnitustracker.mvp.ui.screens.ReferralScreen
import com.tinnitustracker.mvp.ui.screens.ResultScreen
import com.tinnitustracker.mvp.ui.screens.WelcomeScreen
import com.tinnitustracker.mvp.ui.theme.Palette

@Composable
fun App(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val listening by vm.listening.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = ui.screen != Screen.WELCOME && ui.screen != Screen.MAIN) { vm.back() }

    // Ask for the notification permission on the first play, then start either way.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.togglePlay() }
    val onPlay: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT >= 33 && !playback.playing &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.togglePlay()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        AnimatedContent(
            targetState = ui.screen,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.WELCOME -> WelcomeScreen(onAnswer = vm::answerSafety)
                Screen.REFERRAL -> ReferralScreen(onBack = { vm.back() }, onContinue = vm::continueAfterReferral)
                Screen.PITCH -> PitchScreen(
                    finder = ui.finder,
                    onBack = { vm.back() },
                    onToggleTone = vm::toggleTone,
                    onPitch = vm::setPitch,
                    onStep = vm::stepPitch,
                    onNoClearTone = vm::noClearTone,
                    onNext = vm::pitchDone,
                )
                Screen.OCTAVE -> OctaveScreen(ui.finder, onBack = { vm.back() }, onTry = vm::tryOctave, onNext = vm::octaveDone)
                Screen.LOUDNESS -> LoudnessScreen(
                    finder = ui.finder,
                    onBack = { vm.back() },
                    onToggleTone = vm::toggleTone,
                    onLoudness = vm::setLoudness,
                    onStep = vm::stepLoudness,
                    onDone = vm::loudnessDone,
                )
                Screen.RESULT -> ResultScreen(ui.finder, onBack = { vm.back() }, onRedo = vm::redo, onSave = vm::save)
                Screen.MAIN -> MainScreen(
                    profile = ui.profile,
                    checkIns = ui.checkIns,
                    mixingPoint = ui.mixingPoint,
                    playback = playback,
                    timer = timer,
                    volume = volume,
                    volumeMax = vm.volumeMax,
                    message = ui.message,
                    onFind = vm::findAgain,
                    onCheckIn = vm::openCheckIn,
                    onRecords = vm::openRecords,
                    onInfo = vm::openInfo,
                    onSound = vm::selectSound,
                    onNotch = vm::toggleNotch,
                    onPlay = onPlay,
                    onVolume = vm::setVolume,
                    onMix = vm::openMix,
                    onTimer = vm::openTimer,
                )
                Screen.FLAGS -> FlagsScreen(onBack = { vm.back() })
                Screen.MIX -> MixScreen(
                    mix = ui.mix,
                    volumeMax = vm.volumeMax,
                    playing = playback.playing,
                    soundName = playback.sound.fullName,
                    notchActive = playback.notchActive,
                    onBack = { vm.back() },
                    onPlay = onPlay,
                    onLevel = vm::setMixLevel,
                    onStep = vm::stepMixLevel,
                    onSave = vm::saveMix,
                )
                Screen.CHECKIN -> CheckInScreen(ui.checkIn, onBack = { vm.back() }, onAnswer = vm::answerCheckIn, onNext = vm::checkInNext)
                Screen.RECORDS -> RecordsScreen(ui.checkIns, listening, ui.message, onBack = { vm.back() }, onCheckIn = vm::openCheckIn)
            }
        }
        if (ui.infoOpen) InfoSheet(onDismiss = vm::closeInfo, onFlags = vm::openFlags)
        if (ui.timerOpen) TimerSheet(selected = timer.minutes, onSelect = vm::setTimer, onDismiss = vm::closeTimer)
    }
}
