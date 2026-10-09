package com.arise.hunter

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest

import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.DataOrigin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*

class HunterModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("native-v3", Context.MODE_PRIVATE)
    private val initial = Progress(day = prefs.getString("day", "") ?: "", xp = prefs.getInt("xp", 0), dayLevel = prefs.getInt("dailyLevel", 1), steps = if(prefs.contains("samsungOnly")) prefs.getLong("steps", 0) else 0, km = if(prefs.contains("samsungOnly")) Double.fromBits(prefs.getLong("km", 0)) else 0.0, earned = prefs.getInt("earned", 0), manual = prefs.getInt("manual", 0), lastSync = if(prefs.contains("samsungOnly")) prefs.getLong("sync", 0) else 0, distanceEstimated = prefs.getBoolean("distanceEstimated", false), samsungOnly = prefs.getBoolean("samsungOnly", true)).rollover()
    private val _state = MutableStateFlow(initial)
    val state = _state.asStateFlow()
    private val gate = Mutex()
    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class), HealthPermission.getReadPermission(DistanceRecord::class))
    fun availability() = HealthConnectClient.getSdkStatus(getApplication())
    private fun save(s: Progress) {
        _state.value = s
        prefs.edit().putString("day", s.day).putInt("xp", s.xp).putInt("dailyLevel", s.dayLevel).putLong("steps", s.steps).putLong("km", s.km.toBits()).putInt("earned", s.earned).putInt("manual", s.manual).putLong("sync", s.lastSync).putBoolean("distanceEstimated", s.distanceEstimated).putBoolean("samsungOnly", s.samsungOnly).apply()
    }
    fun selectSamsung(enabled: Boolean) { save(_state.value.copy(samsungOnly = enabled, steps = 0, km = 0.0, lastSync = 0, status = "Source changed. Refresh activity.")); refresh() }
    fun confirm(index: Int, day: String) { save(_state.value.confirm(index, day)) }
    fun refresh() = viewModelScope.launch {
        if (gate.isLocked) return@launch
        gate.withLock {
            save(_state.value.rollover())
            if (availability() != HealthConnectClient.SDK_AVAILABLE) {
                _state.value = _state.value.copy(status = "Install or update Health Connect to import activity", busy = false)
                return@withLock
            }
            val date = _state.value.day
            val samsungOnly = _state.value.samsungOnly
            val origins = if (samsungOnly) setOf(DataOrigin("com.sec.android.app.shealth")) else emptySet()
            _state.value = _state.value.copy(busy = true)
            try {
                val client = HealthConnectClient.getOrCreate(getApplication())
                val granted = client.permissionController.getGrantedPermissions()
                val canSteps = HealthPermission.getReadPermission(StepsRecord::class) in granted
                val canDistance = HealthPermission.getReadPermission(DistanceRecord::class) in granted
                if (!canSteps && !canDistance) {
                    _state.value = _state.value.copy(status = "Allow activity access in Profile", busy = false)
                    return@withLock
                }
                val start = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end = Instant.now()
                val range = TimeRangeFilter.between(start, end)
                val steps = if (canSteps) client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range, origins))[StepsRecord.COUNT_TOTAL] ?: 0L else null
                // Distance records do not require a workout session or a matching step source.
                val distanceRead = if (canDistance) runCatching {
                    client.aggregate(AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), range, origins))[DistanceRecord.DISTANCE_TOTAL]?.inMeters
                } else null
                val distance = DistanceValue.resolve(distanceRead?.getOrNull(), steps)
                val current = _state.value.rollover()
                if (current.day == date && current.samsungOnly == samsungOnly) save(current.copy(steps = steps ?: current.steps, km = distance?.km ?: current.km, distanceEstimated = distance?.estimated ?: current.distanceEstimated, lastSync = System.currentTimeMillis(), busy = false, status = (if(samsungOnly) "Source: Samsung Health. " else "Source: Health Connect combined. ") + when { samsungOnly && steps == 0L -> "No Samsung steps shared for today yet. Sync your watch in Samsung Health and enable Steps sharing with Health Connect."; distance?.estimated == true -> "Distance estimated from steps (0.70 m per step). Actual distance may vary."; distanceRead?.isFailure == true -> "Steps imported. Distance could not be read; showing the last value."; !canDistance -> "Allow distance access in Profile."; distance == null -> "No distance records available yet."; else -> "Daily distance imported from Health Connect" }).award()) else save(current.copy(busy = false))
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Could not import activity. Check Health Connect access and try again.")
            }
        }
    }
}
