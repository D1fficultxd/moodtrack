package com.d1ff.moodtrack.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.ui.screens.ExportScreen
import com.d1ff.moodtrack.ui.screens.HistoryScreen
import com.d1ff.moodtrack.ui.screens.SettingsScreen
import com.d1ff.moodtrack.ui.screens.TodayScreen
import com.d1ff.moodtrack.ui.screens.GuideScreen

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Export : Screen("export", R.string.tab_export, Icons.Filled.FileDownload)
    object Today : Screen("today", R.string.tab_today, Icons.Filled.EditNote)
    object Calendar : Screen("calendar", R.string.tab_calendar, Icons.Filled.CalendarMonth)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Filled.Settings)
    object Guide : Screen("guide", R.string.guide_title, Icons.Filled.EditNote)
    object CalendarEdit : Screen("calendar/edit/{date}", R.string.tab_today, Icons.Filled.EditNote)
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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on main screens
            val isMainScreen = items.any { it.route == currentDestination?.route }
            AnimatedVisibility(
                visible = isMainScreen,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                        )
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(72.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                                    label = null,
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (selected) {
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
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260)
                )
            }
        ) {
            composable(Screen.Export.route) { ExportScreen() }
            composable(Screen.Today.route) {
                TodayScreen(onNavigateToGuide = {
                    navController.navigate(Screen.Guide.route) { launchSingleTop = true }
                })
            }
            composable(Screen.Calendar.route) { 
                HistoryScreen(onNavigateToEdit = { date -> 
                    navController.navigate("calendar/edit/$date")
                }) 
            }
            composable(Screen.CalendarEdit.route) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date")
                TodayScreen(
                    initialDate = date, 
                    onBack = { navController.popBackStack() },
                    onNavigateToGuide = {
                        navController.navigate(Screen.Guide.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Settings.route) { 
                SettingsScreen(onNavigateToGuide = { 
                    navController.navigate(Screen.Guide.route) { launchSingleTop = true }
                }) 
            }
            composable(Screen.Guide.route) {
                GuideScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
