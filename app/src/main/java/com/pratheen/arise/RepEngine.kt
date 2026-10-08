package com.pratheen.arise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.acos
import kotlin.math.sqrt

/** Matches the exercise order the web app already uses: 0 push-ups, 1 sit-ups, 2 squats. */
object Exercise {
    const val PUSHUPS = 0
    const val SITUPS = 1
    const val SQUATS = 2
}

class RepEngine(
    context: Context,
    private val onRep: (Int) -> Unit,
    private val onStarted: (Int, String) -> Unit,
    private val onError: (String) -> Unit
) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var exercise = -1
    private var lastRepAt = 0L
    private val refractoryMs = 700L

    // push-up (proximity) state
    private var wasNear = false

    // sit-up (tilt angle) state
    private var gravity = FloatArray(3)
    private var baselineAngle = 0f
    private var calibrated = false
    private var reclined = false
    private var samples = 0

    // squat (calibrated magnitude dip) state
    private var baselineMag = 9.8f
    private var dipping = false

    fun start(ex: Int) {
        exercise = ex
        lastRepAt = 0L
        wasNear = false
        calibrated = false
        reclined = false
        samples = 0
        dipping = false
        baselineMag = 9.8f

        when (ex) {
            Exercise.PUSHUPS -> {
                if (proximity == null) { onError("This device has no proximity sensor"); return }
                sm.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI)
                onStarted(ex, "Place the phone flat on the floor, proximity sensor up. Lower your face near it, then rise fully.")
            }
            Exercise.SITUPS -> {
                if (accel == null) { onError("This device has no motion sensor"); return }
                sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
                onStarted(ex, "Hold the phone against your chest, screen out. Recline, then sit upright at a steady pace.")
            }
            Exercise.SQUATS -> {
                if (accel == null) { onError("This device has no motion sensor"); return }
                sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
                onStarted(ex, "Secure the phone against your torso. Stand still a moment, then squat down and rise slowly.")
            }
            else -> onError("Unknown exercise")
        }
    }

    fun stop() {
        sm.unregisterListener(this)
        exercise = -1
    }

    override fun onSensorChanged(event: SensorEvent) {
        when {
            exercise == Exercise.PUSHUPS && event.sensor.type == Sensor.TYPE_PROXIMITY -> handleProximity(event)
            exercise == Exercise.SITUPS && event.sensor.type == Sensor.TYPE_ACCELEROMETER -> handleTilt(event)
            exercise == Exercise.SQUATS && event.sensor.type == Sensor.TYPE_ACCELEROMETER -> handleSquat(event)
        }
    }

    private fun cooledDown(now: Long) = now - lastRepAt > refractoryMs

    private fun handleProximity(e: SensorEvent) {
        val range = proximity?.maximumRange ?: 5f
        val isNear = e.values[0] < range * 0.6f
        val now = System.currentTimeMillis()
        // A rep is a full near-then-far cycle: face approaches the phone, then pulls away.
        if (isNear && !wasNear) {
            wasNear = true
        } else if (!isNear && wasNear) {
            wasNear = false
            if (cooledDown(now)) { lastRepAt = now; onRep(Exercise.PUSHUPS) }
        }
    }

    /** Low-pass filter to separate gravity from the raw accelerometer signal. */
    private fun updateGravity(e: SensorEvent) {
        val alpha = 0.85f
        gravity[0] = alpha * gravity[0] + (1 - alpha) * e.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * e.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * e.values[2]
    }

    private fun handleTilt(e: SensorEvent) {
        updateGravity(e)
        val mag = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (mag < 1f) return
        // Angle between gravity and the phone's long (Y) axis swings sharply between sitting
        // upright (phone vertical against the chest) and reclining (phone tips back).
        val angle = Math.toDegrees(acos((gravity[1] / mag).toDouble())).toFloat()

        samples++
        if (!calibrated) {
            // Assume the first ~1.5s at SENSOR_DELAY_GAME (~50Hz) captures the starting pose.
            if (samples > 70) { baselineAngle = angle; calibrated = true }
            return
        }

        val deviation = kotlin.math.abs(angle - baselineAngle)
        val now = System.currentTimeMillis()
        if (deviation > 40f && !reclined) {
            reclined = true
        } else if (deviation < 15f && reclined) {
            reclined = false
            if (cooledDown(now)) { lastRepAt = now; onRep(Exercise.SITUPS) }
        }
    }

    private fun handleSquat(e: SensorEvent) {
        val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
        samples++
        if (!calibrated) {
            if (samples > 70) calibrated = true
            // Slowly settle the baseline toward the calm standing magnitude.
            baselineMag = 0.9f * baselineMag + 0.1f * mag
            return
        }
        val now = System.currentTimeMillis()
        val delta = mag - baselineMag
        if (delta < -1.6f && !dipping) {
            dipping = true
        } else if (delta > -0.4f && dipping) {
            dipping = false
            if (cooledDown(now)) { lastRepAt = now; onRep(Exercise.SQUATS) }
        }
        // Baseline drifts slowly so posture shifts don't cause false positives, but a real
        // squat cycle happens too fast for this filter to absorb it.
        baselineMag = 0.98f * baselineMag + 0.02f * mag
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
