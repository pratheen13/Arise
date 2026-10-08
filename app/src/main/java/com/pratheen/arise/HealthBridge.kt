package com.pratheen.arise

import android.content.Context
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime

object HealthBridge {

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun client(context: Context): HealthConnectClient? =
        if (isAvailable(context)) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(context: Context): Boolean {
        val c = client(context) ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    data class Totals(val steps: Long, val km: Double, val sources: List<String>)

    /** Friendly names for the data-origin packages we're most likely to actually see. */
    private fun friendlyName(context: Context, pkg: String): String {
        val known = mapOf(
            "com.google.android.apps.fitness" to "Google Fit",
            "com.sec.android.app.shealth" to "Samsung Health",
            "com.google.android.wearable.app" to "Wear OS",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.garmin.android.apps.connectmobile" to "Garmin Connect",
            "com.strava" to "Strava",
            "com.pratheen.arise" to "Arise (this phone)"
        )
        known[pkg]?.let { return it }
        return runCatching {
            val pm = context.packageManager
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)
    }

    suspend fun readToday(context: Context): Totals? {
        val c = client(context) ?: return null
        val start = LocalDate.now().atStartOfDay()
        val range = TimeRangeFilter.between(start, LocalDateTime.now())
        return runCatching {
            val res = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = range
                )
            )
            val sources = res.dataOrigins.map { friendlyName(context, it.packageName) }.distinct()
            Totals(
                steps = res[StepsRecord.COUNT_TOTAL] ?: 0L,
                km = (res[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000.0,
                sources = sources
            )
        }.getOrNull()
    }

    const val PLAY_STORE_URI =
        "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
}
