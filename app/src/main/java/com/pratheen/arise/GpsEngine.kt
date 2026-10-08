package com.pratheen.arise

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class Fix(val lat: Double, val lon: Double, val accuracy: Float, val time: Long)

/**
 * Distance math ported 1:1 from the reference app's own filtering (accuracy <= 35m on both
 * fixes, 0-30s between them, movement past the GPS noise floor, speed capped at 12 m/s) so a
 * run tracked here behaves exactly like it did in the original web build.
 */
private fun haversineMeters(a: Fix, b: Fix): Double {
    val toRad = Math.PI / 180
    val dLat = (b.lat - a.lat) * toRad
    val dLon = (b.lon - a.lon) * toRad
    val sinLat = sin(dLat / 2)
    val sinLon = sin(dLon / 2)
    val h = sinLat * sinLat + cos(a.lat * toRad) * cos(b.lat * toRad) * sinLon * sinLon
    return 6371e3 * 2 * atan2(sqrt(h), sqrt(max(0.0, 1 - h)))
}

private fun acceptedDistance(prev: Fix, cur: Fix): Double {
    val seconds = (cur.time - prev.time) / 1000.0
    val meters = haversineMeters(prev, cur)
    val ok = cur.accuracy <= 35f && prev.accuracy <= 35f &&
        seconds > 0 && seconds < 30 &&
        meters >= max(5.0, cur.accuracy * 0.5) &&
        meters / seconds <= 12.0
    return if (ok) meters else 0.0
}

class GpsEngine(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var last: Fix? = null
    private var meters = 0.0
    private var active = false

    private val _km = MutableStateFlow(0.0)
    val km: StateFlow<Double> = _km

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val fix = Fix(loc.latitude, loc.longitude, loc.accuracy, loc.time)
            if (fix.accuracy > 35f) {
                _status.value = "Weak GPS \u00B7 find an open area"
                return
            }
            _status.value = "GPS locked \u00B7 \u00B1${fix.accuracy.toInt()} m"
            val prev = last
            if (prev != null) meters += acceptedDistance(prev, fix)
            last = fix
            _km.value = meters / 1000.0
        }
    }

    @SuppressLint("MissingPermission")
    fun start(onError: (String) -> Unit) {
        meters = 0.0; last = null; _km.value = 0.0
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        try {
            client.requestLocationUpdates(req, callback, android.os.Looper.getMainLooper())
            active = true
            _status.value = "Acquiring GPS \u00B7 head outdoors"
        } catch (se: SecurityException) {
            onError("Location permission is required for GPS runs")
        }
    }

    /** Stops tracking and returns the distance covered, in kilometres, for the caller to bank. */
    fun stop(): Double {
        if (active) client.removeLocationUpdates(callback)
        active = false
        val result = meters / 1000.0
        meters = 0.0; last = null; _km.value = 0.0; _status.value = ""
        return result
    }

    val isActive: Boolean get() = active
}
