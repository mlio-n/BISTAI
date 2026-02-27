package com.muzaffer.bistai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.muzaffer.bistai.presentation.portfolio.PortfolioScreen
import com.muzaffer.bistai.ui.theme.BISTAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BISTAITheme {
                PortfolioScreen()
            }
        }
    }
}