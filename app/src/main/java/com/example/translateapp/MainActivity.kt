package com.example.translateapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.translateapp.service.AppSettingsManager
import com.example.translateapp.ui.MainScreen
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.google.android.gms.ads.MobileAds
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
