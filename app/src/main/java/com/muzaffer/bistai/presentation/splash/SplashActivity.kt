package com.muzaffer.bistai.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzaffer.bistai.MainActivity
import com.muzaffer.bistai.ui.theme.BullishGreen
import com.muzaffer.bistai.ui.theme.GoldAccent
import com.muzaffer.bistai.ui.theme.NavyBlueMedium
import com.muzaffer.bistai.ui.theme.PureBlack
import com.muzaffer.bistai.ui.theme.SlateBlue
import com.muzaffer.bistai.ui.theme.White
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
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "fade_in"
    )
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "line_draw"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
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
                    colors = listOf(PureBlack, NavyBlueMedium, PureBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Arka plan ızgara noktaları (subtle) ──────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 60f
            val dotColor = Color(0xFF0A1628)
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = 2f, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.alpha(alpha)
        ) {

            // ── Yükselen trend grafiği ───────────────────────────────────
            Canvas(
                modifier = Modifier
                    .size(width = 160.dp, height = 90.dp)
                    .padding(bottom = 8.dp)
            ) {
                val w = size.width
                val h = size.height

                // Referans çizgileri
                for (i in 1..3) {
                    drawLine(
                        color = Color(0xFF0A192F),
                        start = Offset(0f, h * i / 4),
                        end = Offset(w, h * i / 4),
                        strokeWidth = 1f
                    )
                }

                // Trend çizgisi noktaları
                val points = listOf(
                    Offset(0f * w, 0.85f * h),
                    Offset(0.2f * w, 0.65f * h),
                    Offset(0.4f * w, 0.75f * h),
                    Offset(0.6f * w, 0.40f * h),
                    Offset(0.8f * w, 0.50f * h),
                    Offset(1.0f * w, 0.15f * h)
                )

                // Dolgu alanı (gradient)
                val fillPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BullishGreen.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // Ana çizgi
                val linePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    linePath,
                    color = BullishGreen,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Veri noktaları
                points.forEachIndexed { i, pt ->
                    val dotColor = if (i == points.size - 1) GoldAccent else BullishGreen
                    val dotRadius = if (i == points.size - 1) 7f else 5f
                    drawCircle(color = PureBlack, radius = dotRadius + 2f, center = pt)
                    drawCircle(color = dotColor, radius = dotRadius, center = pt)
                }

                // Parlayan son nokta (zirve)
                drawCircle(
                    color = BullishGreen.copy(alpha = glowAlpha * 0.4f),
                    radius = 18f,
                    center = points.last()
                )
            }

            Spacer(Modifier.height(12.dp))

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
                            colors = listOf(Color.Transparent, BullishGreen, Color.Transparent)
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
                        .background(BullishGreen, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    "CANLI",
                    fontSize = 9.sp,
                    color = BullishGreen,
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
