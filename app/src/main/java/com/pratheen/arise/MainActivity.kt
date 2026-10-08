package com.pratheen.arise

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pratheen.arise.ui.*

class MainActivity : ComponentActivity() {

    private val vm: AriseViewModel by viewModels()

    private val activityRecognitionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.beginStepTracking() else vm.toast.value = "Step permission was not granted"
        }

    private val locationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) vm.beginGpsRun() else vm.toast.value = "Location permission was not granted"
        }

    private val healthLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            vm.onHealthPermissionResult(granted.containsAll(HealthBridge.PERMISSIONS))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        vm.checkHealthConnectedOnLaunch()
        vm.startPeriodicHealthSync()
        if (hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) vm.beginStepTracking()

        setContent { AriseApp(vm, ::connectPhone, ::connectHealth, ::toggleGps) }
    }

    private fun hasPermission(p: String) =
        androidx.core.content.ContextCompat.checkSelfPermission(this, p) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun connectPhone() {
        if (hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) vm.beginStepTracking()
        else activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun connectHealth() {
        if (!HealthBridge.isAvailable(this)) {
            runCatching {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(HealthBridge.PLAY_STORE_URI)).apply {
                    setPackage("com.android.vending")
                })
            }
            return
        }
        healthLauncher.launch(HealthBridge.PERMISSIONS)
    }

    private fun toggleGps() {
        if (vm.gpsRunning.value) { vm.endGpsRun(); return }
        val fine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine || coarse) vm.beginGpsRun()
        else locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onPause() { super.onPause(); vm.stopStepTrackingForBackground() }
    override fun onResume() { super.onResume(); vm.resumeStepTrackingIfConnected() }
}

@Composable
fun AriseApp(vm: AriseViewModel, onConnectPhone: () -> Unit, onConnectHealth: () -> Unit, onToggleGps: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val activeExercise by vm.activeExercise.collectAsStateWithLifecycle()
    val sessionMessage by vm.sessionMessage.collectAsStateWithLifecycle()
    val sessionCount by vm.sessionCount.collectAsStateWithLifecycle()
    val stepsConnected by vm.stepsConnected.collectAsStateWithLifecycle()
    val healthConnected by vm.healthConnected.collectAsStateWithLifecycle()
    val healthSources by vm.healthSources.collectAsStateWithLifecycle()
    val syncedAt by vm.syncedAt.collectAsStateWithLifecycle()
    val gpsRunning by vm.gpsRunning.collectAsStateWithLifecycle()
    val gpsKm by vm.gps.km.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Ink.Void)) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (screen) {
                    "hunter" -> HunterScreen(state) { vm.setScreen("quests") }
                    "quests" -> QuestsScreen(
                        state = state, stepsConnected = stepsConnected,
                        onTrackReps = { vm.beginRepSession(it) },
                        gpsRunning = gpsRunning, liveExtraKm = if (gpsRunning) gpsKm else 0.0, onToggleGps = onToggleGps,
                        onManageSources = { vm.setScreen("connect") },
                        onToggleRest = { vm.toggleRest() }
                    )
                    "connect" -> ConnectScreen(
                        stepsConnected = stepsConnected, onConnectPhone = onConnectPhone,
                        healthConnected = healthConnected, onConnectHealth = onConnectHealth,
                        onSyncHealth = { vm.syncHealth() },
                        sources = healthSources, syncedAt = syncedAt,
                        onPrivacy = { vm.toast.value = "Steps, GPS and reps stay on this device; Health Connect data is read only when you sync." }
                    )
                    "rank" -> RankScreen(state)
                }
            }
            BottomNav(screen) { vm.setScreen(it) }
        }

        activeExercise?.let { ex ->
            SessionPanel(ex, sessionMessage, sessionCount) { vm.endRepSession() }
        }
        toast?.let { ToastBanner(it) { vm.dismissToast() } }
    }
}
