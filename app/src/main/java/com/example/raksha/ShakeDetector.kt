package com.example.raksha

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Improved ShakeDetector — 3-layer false positive prevention:
 *
 * Layer 1: Higher threshold (18g vs old 13g) — ignores vehicle vibration
 * Layer 2: Requires 3 strong shakes within 2 seconds — accidental single shake ignored
 * Layer 3: 5-second cooldown after trigger — prevents repeated firing
 *
 * A person in genuine distress CAN shake 3 times quickly.
 * A bus/train vibration is continuous low-level, NOT 3 distinct peaks.
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Layer 1: Higher threshold — vehicle vibration is typically 10-14g
    // Emergency shake is typically 20-25g
    private val shakeThreshold = 18.0f

    // Layer 2: Need 3 shakes within this window
    private val shakeWindowMs = 2000L   // 2 seconds window
    private val requiredShakes = 3      // must shake 3 times

    // Layer 3: Cooldown after trigger
    private val cooldownMs = 5000L

    private val shakeTimes = mutableListOf<Long>()
    private var lastTriggerTime = 0L
    private var lastShakeTime = 0L
    private val minTimeBetweenShakes = 200L // prevent same shake counted twice

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        shakeTimes.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gForce = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // Must be above threshold
        if (gForce < shakeThreshold) return

        // Prevent same shake counted twice
        if (now - lastShakeTime < minTimeBetweenShakes) return
        lastShakeTime = now

        // Add this shake timestamp
        shakeTimes.add(now)

        // Remove shakes outside the 2-second window
        shakeTimes.removeAll { now - it > shakeWindowMs }

        // Check if we have enough shakes in the window
        if (shakeTimes.size >= requiredShakes) {
            // Check cooldown
            if (now - lastTriggerTime > cooldownMs) {
                lastTriggerTime = now
                shakeTimes.clear()
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}