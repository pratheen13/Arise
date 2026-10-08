package com.pratheen.arise

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDate

/**
 * TYPE_STEP_COUNTER reports a raw count since the device's last boot, not "steps today."
 * To get today's total we remember the sensor value that corresponded to zero steps today
 * (the "baseline") and subtract it from every new reading. The baseline is persisted so it
 * survives the activity restarting; it resets whenever the date changes, and re-anchors
 * itself if the raw counter is ever lower than the stored baseline (a reboot zeroes the
 * hardware counter).
 */
class StepEngine(context: Context, private val onTotal: (Int) -> Unit, private val onError: (String) -> Unit) :
    SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs: SharedPreferences = context.getSharedPreferences("arise_steps", Context.MODE_PRIVATE)

    private var listening = false

    fun start() {
        if (sensor == null) {
            onError("This device has no step counter sensor")
            return
        }
        listening = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (!listening) onError("Could not start the step sensor")
    }

    fun stop() {
        if (listening) sm.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values[0].toInt()
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("date", null)
        var baseline = prefs.getInt("baseline", -1)

        if (savedDate != today || baseline < 0 || raw < baseline) {
            baseline = raw
            prefs.edit().putString("date", today).putInt("baseline", baseline).apply()
        }
        onTotal((raw - baseline).coerceAtLeast(0))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
