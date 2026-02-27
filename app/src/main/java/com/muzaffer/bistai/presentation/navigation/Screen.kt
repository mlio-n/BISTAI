package com.muzaffer.bistai.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Uygulamadaki tüm ekran rotalarını tanımlayan sealed class.
 */
sealed class Screen(val route: String) {
    data object Portfolio  : Screen("portfolio")
    data object Favorites  : Screen("favorites")
    data object AiChat     : Screen("ai_chat")
    data object StockDetail : Screen("stock_detail/{symbol}") {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
    }
}

/**
 * Alt navigasyon sekmelerini temsil eden enum.
 */
enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PORTFOLIO(
        screen        = Screen.Portfolio,
        label         = "Portföy",
        selectedIcon  = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart
    ),
    FAVORITES(
        screen        = Screen.Favorites,
        label         = "Favorilerim",
        selectedIcon  = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    AI_CHAT(
        screen        = Screen.AiChat,
        label         = "BISTAI",
        selectedIcon  = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )
}
