package com.pratheen.arise

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.round

val Context.dataStore by preferencesDataStore("arise")

/** 0 push-ups, 1 sit-ups, 2 squats — the fixed exercise order used throughout. */
val EXERCISE_NAMES = listOf("Push-ups", "Sit-ups", "Squats")

data class RankInfo(val rank: String, val title: String, val level: Int)

val RANKS = listOf(
    RankInfo("E", "Awakening", 1),
    RankInfo("D", "Rising hunter", 10),
    RankInfo("C", "Dungeon challenger", 25),
    RankInfo("B", "Elite hunter", 45),
    RankInfo("A", "Master hunter", 70),
    RankInfo("S", "Shadow monarch", 100)
)

data class QuestTarget(val reps: Int, val km: Double, val steps: Int)

/** Raw sensor/health inputs before they're folded into the day's official progress. */
data class Signals(
    val phoneSteps: Int = 0,
    val healthSteps: Int = 0,
    val gpsKm: Double = 0.0,
    val healthKm: Double = 0.0,
    val phoneReps: List<Int> = listOf(0, 0, 0),
    val healthReps: List<Int> = listOf(0, 0, 0),
    val syncedAt: Long = 0L
)

data class HunterState(
    val date: String = LocalDate.now().toString(),
    val xp: Int = 0,
    val days: Int = 0,
    val cleared: Int = 0,
    val reps: List<Int> = listOf(0, 0, 0),
    val km: Double = 0.0,
    val steps: Int = 0,
    val earned: List<Boolean> = listOf(false, false, false, false, false),
    val rested: Boolean = false,
    val signals: Signals = Signals(),
    // Snapshotted from `days` when the state is created (app open / day rollover) — NOT a
    // live getter. If it were recomputed from `days` on every read, clearing all 5 tasks
    // (which bumps `days`) would silently raise the target again for the rest of the same
    // day, causing already-earned tasks to un-clear and XP to be deducted moments later.
    val target: QuestTarget = questTarget(days)
) {
    val level: Int get() = min(100, 1 + floor(xp / 200.0).toInt())
    val rankInfo: RankInfo get() = RANKS.last { level >= it.level }

    /** Fraction (0f..1f) of XP progress toward the next level, for a progress bar. */
    val xpFraction: Float get() = if (level >= 100) 1f else (xp % 200) / 200f
}

/** Targets scale smoothly from day 0 to day 99, reaching the canonical 100/100/100 + 10km. */
fun questTarget(days: Int): QuestTarget {
    val t = min(99, days) / 99.0
    val reps = 5 + floor(95 * t).toInt()
    val km = round((0.5 + 9.5 * t) * 20) / 20.0
    val steps = 1000 + (floor(9000 * t / 100) * 100).toInt()
    return QuestTarget(reps, km, steps)
}

/** Re-derives `earned` from current progress and awards 40 XP for each newly-cleared task. */
fun HunterState.recomputed(): HunterState {
    if (rested) return this
    val t = target
    val newEarned = listOf(
        reps[0] >= t.reps, reps[1] >= t.reps, reps[2] >= t.reps,
        km + 1e-8 >= t.km, steps >= t.steps
    )
    val newlyCleared = newEarned.count { it } - earned.count { it }
    val allNow = newEarned.all { it }
    val allBefore = earned.all { it }
    return copy(
        earned = newEarned,
        xp = xp + newlyCleared * 40,
        cleared = cleared + newlyCleared,
        days = days + if (allNow && !allBefore) 1 else 0
    )
}

/** Rolls the signals into official progress (larger of phone vs. health wins, never summed). */
fun HunterState.withSignals(s: Signals): HunterState = copy(
    signals = s,
    steps = maxOf(s.phoneSteps, s.healthSteps),
    km = maxOf(s.gpsKm, s.healthKm),
    reps = s.phoneReps.indices.map { maxOf(s.phoneReps[it], s.healthReps[it]) }
).recomputed()

/** Rolls the date forward if a new day has started, carrying over days/cleared/xp. */
fun HunterState.forToday(): HunterState {
    val today = LocalDate.now().toString()
    return if (date == today) this else HunterState(date = today, xp = xp, days = days, cleared = cleared)
}

object HunterRepo {
    private val KEY = stringPreferencesKey("state")

    fun flow(context: Context): Flow<HunterState> = context.dataStore.data.map { decode(it[KEY]) }

    suspend fun save(context: Context, state: HunterState) {
        context.dataStore.edit { it[KEY] = encode(state) }
    }

    private fun encode(s: HunterState): String = JSONObject().apply {
        put("date", s.date); put("xp", s.xp); put("days", s.days); put("cleared", s.cleared)
        put("reps", JSONArray(s.reps)); put("km", s.km); put("steps", s.steps)
        put("earned", JSONArray(s.earned)); put("rested", s.rested)
        put("target", JSONObject().apply {
            put("reps", s.target.reps); put("km", s.target.km); put("steps", s.target.steps)
        })
        put("signals", JSONObject().apply {
            put("phoneSteps", s.signals.phoneSteps); put("healthSteps", s.signals.healthSteps)
            put("gpsKm", s.signals.gpsKm); put("healthKm", s.signals.healthKm)
            put("phoneReps", JSONArray(s.signals.phoneReps)); put("healthReps", JSONArray(s.signals.healthReps))
            put("syncedAt", s.signals.syncedAt)
        })
    }.toString()

    private fun decode(raw: String?): HunterState {
        if (raw.isNullOrBlank()) return HunterState()
        return runCatching {
            val o = JSONObject(raw)
            val sig = o.optJSONObject("signals") ?: JSONObject()
            val tgt = o.optJSONObject("target")
            fun ints(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.optInt(it) }
            val decodedDays = o.optInt("days", 0)
            val base = HunterState(
                date = o.optString("date", LocalDate.now().toString()),
                xp = o.optInt("xp", 0), days = decodedDays, cleared = o.optInt("cleared", 0),
                reps = ints(o.optJSONArray("reps")).ifEmpty { listOf(0, 0, 0) },
                km = o.optDouble("km", 0.0), steps = o.optInt("steps", 0),
                earned = (0 until (o.optJSONArray("earned")?.length() ?: 0))
                    .map { o.optJSONArray("earned")!!.optBoolean(it) }.ifEmpty { listOf(false, false, false, false, false) },
                rested = o.optBoolean("rested", false),
                // Fall back to deriving it from `days` only for saves written before this field
                // existed — any state saved after this fix always carries its own snapshot.
                target = if (tgt != null) {
                    QuestTarget(tgt.optInt("reps"), tgt.optDouble("km"), tgt.optInt("steps"))
                } else questTarget(decodedDays),
                signals = Signals(
                    phoneSteps = sig.optInt("phoneSteps"), healthSteps = sig.optInt("healthSteps"),
                    gpsKm = sig.optDouble("gpsKm", 0.0), healthKm = sig.optDouble("healthKm", 0.0),
                    phoneReps = ints(sig.optJSONArray("phoneReps")).ifEmpty { listOf(0, 0, 0) },
                    healthReps = ints(sig.optJSONArray("healthReps")).ifEmpty { listOf(0, 0, 0) },
                    syncedAt = sig.optLong("syncedAt", 0L)
                )
            )
            base.forToday()
        }.getOrDefault(HunterState())
    }
}
