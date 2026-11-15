package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ai.YogaAiGenerator
import com.example.data.repository.LaxafitRepository
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GuidedSessionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PersonalizeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RoutineDetailScreen
import com.example.ui.screens.SportWorkoutTrackerScreen
import com.example.ui.screens.SportsScreen
import com.example.ui.screens.WatchH9Screen
import com.example.ui.theme.LaxafitTheme
import com.example.ui.viewmodel.LaxafitViewModel
import com.example.ui.viewmodel.UiState
import com.example.watch.WatchH9BleManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private const val ROUTE_HOME = "home"
private const val ROUTE_PERSONALIZE = "personalize"
private const val ROUTE_SPORTS = "sports"
private const val ROUTE_WATCH = "watch"
private const val ROUTE_PROGRESS = "progress"
private const val ROUTE_ROUTINE_DETAIL = "routine_detail"
private const val ROUTE_SESSION = "session"
private const val ROUTE_SPORT_TRACKER = "sport_tracker"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val mainTabs = listOf(
    NavTab(ROUTE_HOME, "Today", Icons.Rounded.Home, Icons.Outlined.Home),
    NavTab(ROUTE_PERSONALIZE, "For You", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavTab(ROUTE_SPORTS, "Sports", Icons.Rounded.DirectionsRun, Icons.Outlined.DirectionsRun),
    NavTab(ROUTE_WATCH, "Devices", Icons.Rounded.Watch, Icons.Outlined.Watch),
    NavTab(ROUTE_PROGRESS, "Progress", Icons.Rounded.Insights, Icons.Outlined.Insights),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before setContent, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            LaxafitTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            LaxafitMainApp(
                currentUserId = user.uid,
                initialDisplayName = user.displayName ?: "Beginner Yogi",
            )
        }
    }
}

@Composable
private fun LaxafitMainApp(
    currentUserId: String,
    initialDisplayName: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LaxafitViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                LaxafitViewModel(
                    repository = LaxafitRepository(app),
                    aiGenerator = YogaAiGenerator(),
                    watchManager = WatchH9BleManager(app),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName,
                )
            }
        }
    )

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.watchManager.disconnect()
        }
    }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val watchState by viewModel.watchState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val selectedRoutine by viewModel.selectedRoutine.collectAsStateWithLifecycle()
    val selectedSportMode by viewModel.selectedSportMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showNavigationChrome = currentRoute != ROUTE_SESSION &&
        currentRoute != ROUTE_ROUTINE_DETAIL &&
        currentRoute != ROUTE_SPORT_TRACKER

    val profile = (dashboardState as? UiState.Success)?.data?.profile
    val sessions = (dashboardState as? UiState.Success)?.data?.sessions ?: emptyList()
    val watchSyncs = (dashboardState as? UiState.Success)?.data?.watchSyncs ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigationChrome && !useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                dashboardState = dashboardState,
                                watchState = watchState,
                                onOpenRoutine = { routine ->
                                    viewModel.selectRoutine(routine)
                                    navController.navigate(ROUTE_ROUTINE_DETAIL)
                                },
                                onStartPractice = { routine ->
                                    viewModel.selectRoutine(routine)
                                    viewModel.pushRoutineToWatchH9(routine)
                                    navController.navigate(ROUTE_SESSION)
                                },
                                onToggleFavorite = { routine ->
                                    viewModel.toggleFavorite(routine)
                                },
                                onNavigateToPersonalize = {
                                    navController.navigate(ROUTE_PERSONALIZE) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToSports = {
                                    navController.navigate(ROUTE_SPORTS) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToWatch = {
                                    navController.navigate(ROUTE_WATCH) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }

                        composable(ROUTE_SPORTS) {
                            SportsScreen(
                                watchState = watchState,
                                onStartSportWorkout = { mode ->
                                    viewModel.selectSportMode(mode)
                                    viewModel.pushSportModeToWatch(mode)
                                    navController.navigate(ROUTE_SPORT_TRACKER)
                                },
                            )
                        }

                        composable(ROUTE_PERSONALIZE) {
                            PersonalizeScreen(
                                profile = profile,
                                watchState = watchState,
                                aiState = aiState,
                                onGenerateRoutine = { focus, level, mins, note, useH9 ->
                                    viewModel.generatePersonalizedRoutine(
                                        focusArea = focus,
                                        experienceLevel = level,
                                        durationMinutes = mins,
                                        userNote = note,
                                        useWatchH9Metrics = useH9,
                                    )
                                },
                                onOpenGeneratedRoutine = { routine ->
                                    viewModel.selectRoutine(routine)
                                    navController.navigate(ROUTE_ROUTINE_DETAIL)
                                },
                                onStartGeneratedRoutine = { routine ->
                                    viewModel.selectRoutine(routine)
                                    viewModel.pushRoutineToWatchH9(routine)
                                    navController.navigate(ROUTE_SESSION)
                                },
                                onResetAiState = { viewModel.resetAiState() },
                            )
                        }

                        composable(ROUTE_WATCH) {
                            WatchH9Screen(
                                watchManager = viewModel.watchManager,
                                watchState = watchState,
                                profile = profile,
                                syncHistory = watchSyncs,
                                onSyncTelemetryToCloud = { name, addr, hr, bat, steps, source ->
                                    viewModel.syncWatchH9ToCloud(
                                        deviceName = name,
                                        deviceAddress = addr,
                                        heartRateBpm = hr,
                                        batteryPercent = bat,
                                        stepsToday = steps,
                                        source = source,
                                    )
                                },
                            )
                        }

                        composable(ROUTE_PROGRESS) {
                            ProgressScreen(
                                profile = profile,
                                sessions = sessions,
                                onSavePreferences = { name, level, goal, mins ->
                                    viewModel.updateBeginnerPreferences(
                                        displayName = name,
                                        experienceLevel = level,
                                        primaryGoal = goal,
                                        preferredMinutes = mins,
                                    )
                                },
                                onSignOut = {
                                    viewModel.watchManager.disconnect()
                                    signOutUser(credentialManager, scope)
                                },
                            )
                        }

                        composable(ROUTE_ROUTINE_DETAIL) {
                            val routine = selectedRoutine
                            if (routine != null) {
                                RoutineDetailScreen(
                                    routine = routine,
                                    watchState = watchState,
                                    onBack = { navController.popBackStack() },
                                    onStartPractice = {
                                        viewModel.pushRoutineToWatchH9(routine)
                                        navController.navigate(ROUTE_SESSION)
                                    },
                                    onPushToWatchH9 = {
                                        viewModel.pushRoutineToWatchH9(routine)
                                    },
                                    onDeleteRoutine = {
                                        viewModel.deleteSavedRoutine(routine.id)
                                        navController.popBackStack()
                                    },
                                )
                            }
                        }

                        composable(ROUTE_SESSION) {
                            val routine = selectedRoutine
                            if (routine != null) {
                                GuidedSessionScreen(
                                    routine = routine,
                                    watchState = watchState,
                                    onExit = { navController.popBackStack() },
                                    onCompleteAndLog = { mins, posesCount, avgHr, peakHr, synced, feeling ->
                                        viewModel.completePracticeSession(
                                            routine = routine,
                                            actualMinutes = mins,
                                            posesCompleted = posesCount,
                                            avgHr = avgHr,
                                            peakHr = peakHr,
                                            syncedFromWatch = synced,
                                            feeling = feeling,
                                            onSaved = {
                                                navController.popBackStack(ROUTE_HOME, inclusive = false)
                                            },
                                        )
                                    },
                                )
                            }
                        }

                        composable(ROUTE_SPORT_TRACKER) {
                            val mode = selectedSportMode
                            if (mode != null) {
                                SportWorkoutTrackerScreen(
                                    sportMode = mode,
                                    watchState = watchState,
                                    onExit = { navController.popBackStack() },
                                    onCompleteAndSave = { mins, avgHr, peakHr, synced, feeling ->
                                        viewModel.completeSportWorkout(
                                            mode = mode,
                                            durationMinutes = mins,
                                            avgHr = avgHr,
                                            peakHr = peakHr,
                                            syncedFromWatch = synced,
                                            feeling = feeling,
                                            onSaved = {
                                                navController.popBackStack(ROUTE_SPORTS, inclusive = false)
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
