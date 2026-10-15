package com.lockapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockapp.ui.screens.HomeScreen

/**
 * Navigation graph chính của LockApp.
 * Quản lý điều hướng giữa các màn hình.
 */
@Composable
fun LockAppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAppList = {
                    navController.navigate(Screen.AppList.route)
                },
                onNavigateToIntruderLog = {
                    navController.navigate(Screen.IntruderLog.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.AppList.route) {
            // TODO: Giai đoạn 3 - AppListScreen
        }

        composable(Screen.IntruderLog.route) {
            // TODO: Giai đoạn 3 - IntruderLogScreen
        }

        composable(Screen.Settings.route) {
            // TODO: Giai đoạn 4 - SettingsScreen
        }

        composable(Screen.Setup.route) {
            // TODO: Giai đoạn 3 - SetupScreen
        }
    }
}

/**
 * Sealed class định nghĩa các route màn hình.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AppList : Screen("app_list")
    object IntruderLog : Screen("intruder_log")
    object Settings : Screen("settings")
    object Setup : Screen("setup")
}
