package com.lockapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockapp.ui.screens.AppListScreen
import com.lockapp.ui.screens.HomeScreen
import com.lockapp.ui.screens.IntruderLogScreen
import com.lockapp.ui.screens.SetupScreen
import com.lockapp.util.RepairModeManager

/**
 * Navigation graph chính của LockApp.
 * Quản lý điều hướng giữa các màn hình.
 */
@Composable
fun LockAppNavHost() {
    val context = LocalContext.current
    val repairModeManager = remember { RepairModeManager(context) }
    val navController = rememberNavController()

    // Nếu chưa thiết lập lần đầu thì chuyển vào SetupScreen, ngược lại vào HomeScreen
    val startDestination = if (repairModeManager.isSetupComplete()) {
        Screen.Home.route
    } else {
        Screen.Setup.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                    navController.navigate(Screen.Setup.route)
                }
            )
        }

        composable(Screen.AppList.route) {
            AppListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.IntruderLog.route) {
            IntruderLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            // Mở lại màn hình thiết lập / quyền
            SetupScreen(
                onSetupComplete = {
                    navController.popBackStack()
                }
            )
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
