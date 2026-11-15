package com.muzaffer.bistai.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzaffer.bistai.MainActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val NavyDeep = Color(0xFF020C1B)
    val NavyMedium = Color(0xFF0D1F3C)
    val GreenAccent = Color(0xFF00C853)
    val White = Color(0xFFFFFFFF)
    val SlateBlue = Color(0xFF8892B0)

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "fade_in"
    )

    LaunchedEffect(Unit) {
        delay(2800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavyMedium, NavyDeep)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.alpha(alpha)
        ) {
            Icon(
                imageVector = Icons.Rounded.Analytics,
                contentDescription = "BISTAI Logo",
                tint = GreenAccent,
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── "BISTAI" ana yazı ────────────────────────────────────────
            Text(
                text = "BISTAI",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = White,
                textAlign = TextAlign.Center
            )

            // Yeşil alt çizgi vurgusu
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, GreenAccent, Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(12.dp))

            // ── Alt yazı ─────────────────────────────────────────────────
            Text(
                text = "Yapay Zeka Destekli Finans Asistanı",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp,
                color = SlateBlue,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Küçük AI rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GreenAccent, shape = CircleShape)
                )
                Text(
                    "CANLI",
                    fontSize = 9.sp,
                    color = GreenAccent,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Alt logo küçük hint
        Text(
            text = "by Muzaffer Sarıarslan",
            fontSize = 10.sp,
            color = SlateBlue.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
