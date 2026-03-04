package com.example.translateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.translateapp.ui.MainScreen
import com.example.translateapp.ui.theme.TranslateAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let Compose draw behind system bars without requiring Activity 1.8+
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TranslateAppTheme {
                MainScreen()
            }
        }
    }
}