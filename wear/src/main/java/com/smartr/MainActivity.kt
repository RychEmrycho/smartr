package com.smartr

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartr.data.SettingsRepository
import com.smartr.data.ThemePreference
import com.smartr.presentation.DashboardScreen
import com.smartr.presentation.HistoryScreen
import com.smartr.presentation.SettingsScreen
import com.smartr.presentation.VitalityInfoScreen
import com.smartr.presentation.DailyDetailScreen
import com.smartr.presentation.theme.SmartRTheme
import com.smartr.service.OffBodyService
import com.smartr.worker.PassiveRegistrationWorker
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Settings : Screen("settings")
    object VitalityInfo : Screen("vitality_info")
    object DailyDetail : Screen("daily_detail/{dateIso}") {
        fun createRoute(dateIso: String) = "daily_detail/$dateIso"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(applicationContext)

        lifecycleScope.launch {
            settingsRepository.ensureDefaults()
        }

        val historyRepository = com.smartr.data.history.HistoryRepository(applicationContext)
        lifecycleScope.launch {
            historyRepository.reconcileInterruptedEvents()
        }

        val trackingStateRepository = com.smartr.data.TrackingStateRepository(applicationContext)
        lifecycleScope.launch {
            launch {
                trackingStateRepository.state.collect { state ->
                    com.smartr.logic.PassiveRuntimeStore.inactivityState = state
                }
            }
            launch {
                trackingStateRepository.lastDailySteps.collect { steps ->
                    com.smartr.logic.PassiveRuntimeStore.lastDailySteps = steps
                }
            }
            launch {
                trackingStateRepository.isOffBody.collect { isOffBody ->
                    com.smartr.logic.PassiveRuntimeStore.isOffBody = isOffBody
                }
            }
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.BODY_SENSORS] == true) {
                startOffBodyService()
            }
            if (permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "passive_registration",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<PassiveRegistrationWorker>().build()
                )
            }
        }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = SettingsRepository.DEFAULTS)
            val navController = rememberSwipeDismissableNavController()

            SmartRTheme(
                themePreference = settings.theme
            ) {
                AppScaffold {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController)
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(navController)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(Screen.VitalityInfo.route) {
                            VitalityInfoScreen()
                        }
                        composable(Screen.DailyDetail.route) { backStackEntry ->
                            val dateIso = backStackEntry.arguments?.getString("dateIso") ?: ""
                            DailyDetailScreen(dateIso, navController)
                        }
                    }
                }
            }
        }
    }

    private fun startOffBodyService() {
        startService(Intent(this, OffBodyService::class.java))
    }
}
