package com.muzaffer.bistai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.muzaffer.bistai.presentation.navigation.BistaiNavGraph
import com.muzaffer.bistai.ui.theme.BISTAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API — Activity inflate edilmeden önce kurulmalı
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BISTAITheme {
                BistaiNavGraph()
            }
        }
    }
}