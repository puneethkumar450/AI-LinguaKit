package com.puneeth50.ailinguakit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.gms.ads.MobileAds
import com.puneeth50.ailinguakit.service.AppSettingsManager
import com.puneeth50.ailinguakit.ui.MainScreen
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsManager.applySavedConfiguration(this)
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TranslateAppTheme {
                MainScreen()
            }
        }
    }
}
