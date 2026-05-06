package com.d1ff.moodtrack.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.ui.screens.ChartsScreen
import com.d1ff.moodtrack.ui.screens.ExportScreen
import com.d1ff.moodtrack.ui.screens.HistoryScreen
import com.d1ff.moodtrack.ui.screens.SettingsScreen
import com.d1ff.moodtrack.ui.screens.TodayScreen

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Charts : Screen("charts", R.string.tab_charts, Icons.Filled.BarChart)
    object Export : Screen("export", R.string.tab_export, Icons.Filled.FileDownload)
    object Today : Screen("today", R.string.tab_today, Icons.Filled.EditNote)
    object Calendar : Screen("calendar", R.string.tab_calendar, Icons.Filled.CalendarMonth)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Filled.Settings)
}

val items = listOf(
    Screen.Export,
    Screen.Today,
    Screen.Calendar,
    Screen.Settings
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    // Check if current route matches this screen or its children (for today/{date})
                    val isTodayTab = screen is Screen.Today
                    val isCalendarTab = screen is Screen.Calendar
                    
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route == screen.route || (isTodayTab && it.route?.startsWith("today") == true)
                    } == true
                    
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                        label = null, // No text labels as requested
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (selected) {
                                // If already on this tab, pop back to its root
                                navController.popBackStack(screen.route, inclusive = false)
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            // composable(Screen.Charts.route) { ChartsScreen() }
            composable(Screen.Export.route) { ExportScreen() }
            composable(Screen.Today.route) { 
                TodayScreen() 
            }
            composable("${Screen.Today.route}/{date}") { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date")
                TodayScreen(initialDate = date)
            }
            composable(Screen.Calendar.route) { 
                HistoryScreen(onNavigateToEdit = { date -> 
                    navController.navigate("${Screen.Today.route}/$date")
                }) 
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
