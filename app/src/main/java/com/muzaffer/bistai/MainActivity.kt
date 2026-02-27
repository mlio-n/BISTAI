package com.muzaffer.bistai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzaffer.bistai.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BISTAITheme {
                WelcomeScreen()
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PureBlack, NavyBlueMedium)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // App badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BullishGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "● BORSA CANLI",
                    color = BullishGreen,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome greeting
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LightSlate, fontWeight = FontWeight.Normal)) {
                        append("Hoş geldin,\n")
                    }
                    withStyle(SpanStyle(color = White, fontWeight = FontWeight.ExtraBold)) {
                        append("Muzaffer 👋")
                    }
                },
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Portföyün ve piyasaları anlık takip etmeye hazır mısın?",
                style = MaterialTheme.typography.bodyLarge,
                color = SlateBlue
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Market summary cards (placeholder)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MarketTickerCard(
                    symbol = "BIST 100",
                    price = "9.842,31",
                    change = "+1.24%",
                    isBullish = true,
                    modifier = Modifier.weight(1f)
                )
                MarketTickerCard(
                    symbol = "USD/TRY",
                    price = "35,91",
                    change = "-0.38%",
                    isBullish = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // CTA Button
            Button(
                onClick = { /* TODO: Navigate to main screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BullishGreen,
                    contentColor = PureBlack
                )
            ) {
                Text(
                    text = "Piyasalara Git →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MarketTickerCard(
    symbol: String,
    price: String,
    change: String,
    isBullish: Boolean,
    modifier: Modifier = Modifier
) {
    val changeColor = if (isBullish) BullishGreen else BearishRed
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = NavyBlueSurface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.labelMedium,
                color = SlateBlue
            )
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = change,
                style = MaterialTheme.typography.labelLarge,
                color = changeColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF020C1B)
@Composable
fun WelcomeScreenPreview() {
    BISTAITheme(darkTheme = true) {
        WelcomeScreen()
    }
}