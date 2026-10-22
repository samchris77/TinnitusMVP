package com.tinnitustracker.mvp

import android.app.Application
import com.tinnitustracker.mvp.audio.MediaVolume
import com.tinnitustracker.mvp.audio.PlaybackController
import com.tinnitustracker.mvp.data.Prefs
import com.tinnitustracker.mvp.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/** Builds the app's single instances once per process, so every screen and the service share them. */
class MvpApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var prefs: Prefs
        private set
    lateinit var settings: StateFlow<Settings>
        private set
    lateinit var controller: PlaybackController
        private set
    lateinit var volume: MediaVolume
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        // One small file, read before the first frame so no screen has to guess and then change.
        val initial = runBlocking { prefs.settings.first() }
        settings = prefs.settings.stateIn(scope, SharingStarted.Eagerly, initial)
        volume = MediaVolume(this)
        controller = PlaybackController(this, prefs, scope)
        controller.restore(initial)
    }
}
