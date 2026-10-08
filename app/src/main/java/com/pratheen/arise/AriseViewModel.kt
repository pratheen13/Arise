package com.pratheen.arise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AriseViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(HunterState())
    val state: StateFlow<HunterState> = _state

    val screen = MutableStateFlow("hunter")
    val toast = MutableStateFlow<String?>(null)

    // rep session (the modal "automatic tracking" panel)
    val activeExercise = MutableStateFlow<Int?>(null)
    val sessionMessage = MutableStateFlow("")
    val sessionCount = MutableStateFlow(0)
    private var sessionStartCount = 0

    val stepsConnected = MutableStateFlow(false)
    val healthConnected = MutableStateFlow(false)
    val healthSources = MutableStateFlow<List<String>>(emptyList())
    val syncedAt = MutableStateFlow(0L)

    val gpsRunning = MutableStateFlow(false)

    private val stepEngine = StepEngine(
        app,
        onTotal = { total -> updateSignals { copy(phoneSteps = total) } },
        onError = { msg -> toast.value = msg }
    )

    private val repEngine = RepEngine(
        app,
        onRep = { ex ->
            updateSignals { copy(phoneReps = phoneReps.toMutableList().also { it[ex] += 1 }) }
            sessionCount.value += 1
        },
        onStarted = { ex, msg -> activeExercise.value = ex; sessionMessage.value = msg; sessionCount.value = 0; sessionStartCount = 0 },
        onError = { msg -> activeExercise.value = null; toast.value = msg }
    )

    val gps = GpsEngine(app)

    init {
        viewModelScope.launch { _state.value = HunterRepo.flow(app).first().forToday() }
    }

    private fun persist() { viewModelScope.launch { HunterRepo.save(getApplication(), _state.value) } }

    private fun updateState(block: HunterState.() -> HunterState) {
        _state.value = _state.value.forToday().block()
        persist()
    }

    private fun updateSignals(block: Signals.() -> Signals) {
        updateState { withSignals(signals.block()) }
    }

    fun setScreen(s: String) { screen.value = s }
    fun dismissToast() { toast.value = null }

    fun toggleRest() = updateState { copy(rested = !rested).let { if (!it.rested) it.recomputed() else it } }

    // ---- phone steps ----
    fun beginStepTracking() { stepEngine.start(); stepsConnected.value = true }
    fun stopStepTrackingForBackground() { stepEngine.stop() }
    fun resumeStepTrackingIfConnected() { if (stepsConnected.value) stepEngine.start() }

    // ---- rep sessions ----
    fun beginRepSession(exercise: Int) { repEngine.start(exercise) }
    fun endRepSession() {
        val exercise = activeExercise.value ?: 0
        repEngine.stop()
        activeExercise.value = null
        toast.value = "${EXERCISE_NAMES[exercise]} session saved"
    }

    // ---- GPS run ----
    fun beginGpsRun() {
        gps.start(onError = { toast.value = it })
        gpsRunning.value = true
    }
    fun endGpsRun() {
        val km = gps.stop()
        gpsRunning.value = false
        updateSignals { copy(gpsKm = gpsKm + km) }
        toast.value = "Journey saved"
    }

    // ---- Health Connect ----
    fun onHealthPermissionResult(granted: Boolean) {
        healthConnected.value = granted
        if (granted) { toast.value = "Health Connect linked"; syncHealth() }
        else toast.value = "Health Connect permission was not granted"
    }

    fun syncHealth() {
        viewModelScope.launch {
            val app: Application = getApplication()
            if (!HealthBridge.hasPermissions(app)) { healthConnected.value = false; return@launch }
            healthConnected.value = true
            val totals = HealthBridge.readToday(app) ?: return@launch
            healthSources.value = totals.sources
            syncedAt.value = System.currentTimeMillis()
            updateSignals { copy(healthSteps = totals.steps.toInt(), healthKm = totals.km, syncedAt = System.currentTimeMillis()) }
        }
    }

    fun checkHealthConnectedOnLaunch() {
        viewModelScope.launch {
            val app: Application = getApplication()
            if (HealthBridge.hasPermissions(app)) { healthConnected.value = true; syncHealth() }
        }
    }

    // periodic re-sync while the app is open, mirroring the reference app's 60s tick
    fun startPeriodicHealthSync() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (healthConnected.value) syncHealth()
            }
        }
    }
}
