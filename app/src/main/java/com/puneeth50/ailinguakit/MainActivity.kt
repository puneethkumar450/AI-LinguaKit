package com.puneeth50.ailinguakit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.android.gms.ads.MobileAds
import com.puneeth50.ailinguakit.service.AppSettingsManager
import com.puneeth50.ailinguakit.ui.CreativeSplashOverlay
import com.puneeth50.ailinguakit.ui.MainScreen
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AppSettingsManager.applySavedConfiguration(this)
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var showLaunchSplash by rememberSaveable { mutableStateOf(true) }
            TranslateAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                    if (showLaunchSplash) {
                        CreativeSplashOverlay(
                            onFinished = { showLaunchSplash = false }
                        )
                    }
                }
            }
        }
    }
}
