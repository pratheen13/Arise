package com.arise.hunter

import java.time.LocalDate

data class Progress(
    val day: String = LocalDate.now().toString(), val xp: Int = 0,
    val dayLevel: Int = 1, val steps: Long = 0, val km: Double = 0.0,
    val earned: Int = 0, val manual: Int = 0, val lastSync: Long = 0,
    val samsungOnly: Boolean = true, val distanceEstimated: Boolean = false, val status: String = "Connect Health Connect in Profile", val busy: Boolean = false
) {
    val level get() = 1 + xp / 200
    val rank get() = ((level - 1) / 10).coerceIn(0, 5)
    val reps get() = (5 + (dayLevel - 1) / 2 * 5).coerceAtMost(100)
    val stepTarget get() = (1500 + (dayLevel - 1) * 250).coerceAtMost(10000)
    val distanceTarget get() = (0.5 + (dayLevel - 1) * .25).coerceAtMost(10.0)
    val completed get() = Integer.bitCount(earned)
    fun rollover(today: String = LocalDate.now().toString()): Progress = if (today == day) this else copy(day = today, dayLevel = level, steps = 0, km = 0.0, distanceEstimated = false, earned = 0, manual = 0, lastSync = 0)
    fun award(): Progress {
        val completed = (manual and 28) or (if (steps >= stepTarget) 1 else 0) or (if (km >= distanceTarget) 2 else 0)
        val fresh = completed and earned.inv() and 31
        return copy(xp = xp + Integer.bitCount(fresh) * 40, earned = earned or fresh)
    }
    fun confirm(index: Int, confirmationDay: String): Progress {
        val current = rollover()
        if (confirmationDay != current.day || index !in 0..2) return current
        return current.copy(manual = current.manual or (4 shl index)).award()
    }
}
val RankNames = listOf("E", "D", "C", "B", "A", "S")
val ExerciseNames = listOf("Push-ups", "Sit-ups", "Squats")

data class DistanceValue(val km: Double, val estimated: Boolean) {
    companion object {
        fun resolve(meters: Double?, steps: Long?): DistanceValue? = when {
            meters != null && meters.isFinite() && meters > 0 -> DistanceValue(meters / 1000.0, false)
            steps != null && steps > 0 -> DistanceValue(steps * 0.70 / 1000.0, true)
            meters == 0.0 || steps == 0L -> DistanceValue(0.0, false)
            else -> null
        }
    }
}
