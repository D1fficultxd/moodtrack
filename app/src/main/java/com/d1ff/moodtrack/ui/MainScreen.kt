package com.d1ff.moodtrack.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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

private val rootScreens = listOf(
    Screen.Export,
    Screen.Today,
    Screen.Calendar,
    Screen.Settings
)

private val rootRoutes = rootScreens.map { it.route }.toSet()

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
            
            // Only show bottom bar on main screens.
            val isMainScreen = currentDestination?.route in rootRoutes
            AnimatedVisibility(
                visible = isMainScreen,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 28.dp)
                        .padding(top = 6.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                        shape = RoundedCornerShape(34.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rootScreens.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                FloatingNavigationItem(
                                    screen = screen,
                                    selected = selected,
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
                if (initialState.destination.route in rootRoutes && targetState.destination.route in rootRoutes) {
                    fadeIn(animationSpec = tween(160)) + scaleIn(
                        initialScale = 0.99f,
                        animationSpec = tween(180)
                    )
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(220)
                    )
                }
            },
            exitTransition = {
                if (initialState.destination.route in rootRoutes && targetState.destination.route in rootRoutes) {
                    fadeOut(animationSpec = tween(120)) + scaleOut(
                        targetScale = 0.99f,
                        animationSpec = tween(140)
                    )
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(220)
                    )
                }
            },
            popEnterTransition = {
                if (initialState.destination.route in rootRoutes && targetState.destination.route in rootRoutes) {
                    fadeIn(animationSpec = tween(160)) + scaleIn(
                        initialScale = 0.99f,
                        animationSpec = tween(180)
                    )
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    )
                }
            },
            popExitTransition = {
                if (initialState.destination.route in rootRoutes && targetState.destination.route in rootRoutes) {
                    fadeOut(animationSpec = tween(120)) + scaleOut(
                        targetScale = 0.99f,
                        animationSpec = tween(140)
                    )
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    )
                }
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

@Composable
private fun FloatingNavigationItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentDescription = stringResource(screen.titleRes)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 64.dp, height = 44.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = screen.icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
