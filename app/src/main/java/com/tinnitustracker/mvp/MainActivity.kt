package com.tinnitustracker.mvp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tinnitustracker.mvp.ui.App
import com.tinnitustracker.mvp.ui.AppViewModel
import com.tinnitustracker.mvp.ui.theme.MvpTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { MvpTheme { App(vm) } }
    }

    override fun onStop() {
        super.onStop()
        // The finder's tone never plays in the background; the chosen sound does.
        if (!isChangingConfigurations) vm.onBackground()
    }
}
