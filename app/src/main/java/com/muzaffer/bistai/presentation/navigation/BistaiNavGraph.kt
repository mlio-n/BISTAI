package com.muzaffer.bistai.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.muzaffer.bistai.presentation.aichat.AiChatScreen
import com.muzaffer.bistai.presentation.favorites.FavoritesScreen
import com.muzaffer.bistai.presentation.portfolio.PortfolioScreen
import com.muzaffer.bistai.presentation.stockdetail.StockDetailScreen
import com.muzaffer.bistai.ui.theme.*

@Composable
fun BistaiNavGraph() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Bottom nav görünür mü? Sadece ana sekmelerde göster
    val bottomNavRoutes = BottomNavItem.entries.map { it.screen.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        containerColor = PureBlack,
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar, enter = fadeIn(), exit = fadeOut()) {
                BistaiBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { item ->
                        val route = item.screen.route
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Portfolio.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Portfolio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Portfolio.route) {
                PortfolioScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }
            composable(Screen.AiChat.route) {
                AiChatScreen()
            }
            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                StockDetailScreen(
                    symbol = symbol,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BistaiBottomBar(
    currentRoute: String?,
    onTabSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = NavyBlueSurface,
        tonalElevation = 0.dp
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = if (item == BottomNavItem.AI_CHAT) GoldAccent else BullishGreen,
                    selectedTextColor   = if (item == BottomNavItem.AI_CHAT) GoldAccent else BullishGreen,
                    unselectedIconColor = SlateBlue,
                    unselectedTextColor = SlateBlue,
                    indicatorColor      = if (item == BottomNavItem.AI_CHAT)
                        GoldAccent.copy(alpha = 0.12f) else BullishGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
