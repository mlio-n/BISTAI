package com.muzaffer.bistai.presentation.navigation

/**
 * Uygulamadaki tüm ekran rotalarını tanımlayan sealed class.
 */
sealed class Screen(val route: String) {
    data object Portfolio : Screen("portfolio")
    data object StockDetail : Screen("stock_detail/{symbol}") {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
    }
}
