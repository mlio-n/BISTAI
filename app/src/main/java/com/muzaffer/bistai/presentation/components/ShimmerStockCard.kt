package com.muzaffer.bistai.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.muzaffer.bistai.ui.theme.NavyBlueMedium
import com.muzaffer.bistai.ui.theme.NavyBlueSurface

/**
 * Tek bir shimmer hisse kartı iskeleti.
 * InfiniteTransition ile soldan sağa kayan ışık efekti üretir.
 */
@Composable
fun ShimmerStockCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -1000f,
        targetValue  =  1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            NavyBlueSurface,
            NavyBlueMedium.copy(alpha = 0.9f),
            NavyBlueSurface.copy(alpha = 0.8f),
            NavyBlueMedium.copy(alpha = 0.6f),
            NavyBlueSurface
        ),
        start = Offset(shimmerOffset - 200f, 0f),
        end   = Offset(shimmerOffset + 200f, 200f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NavyBlueSurface)
    ) {
        // Shimmer kaplama
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush)
        )

        // İskelet satırları
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol: Sembol + İsim
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                SkeletonLine(widthFraction = 0.30f, height = 16)
                SkeletonLine(widthFraction = 0.55f, height = 12)
            }

            // Sağ: Fiyat + Badge
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonLine(widthFraction = 0.20f, height = 16, absoluteWidthDp = 72)
                SkeletonLine(widthFraction = 0.15f, height = 12, absoluteWidthDp = 52, rounded = 8)
            }
        }
    }
}

/**
 * Yardımcı: Tek bir iskelet çizgisi (gri yuvarlak kutu).
 */
@Composable
private fun SkeletonLine(
    widthFraction: Float = 0.5f,
    height: Int = 14,
    absoluteWidthDp: Int? = null,
    rounded: Int = 6
) {
    val widthModifier = if (absoluteWidthDp != null)
        Modifier.width(absoluteWidthDp.dp)
    else
        Modifier.fillMaxWidth(widthFraction)

    Box(
        modifier = widthModifier
            .height(height.dp)
            .clip(RoundedCornerShape(rounded.dp))
            .background(NavyBlueMedium.copy(alpha = 0.6f))
    )
}
