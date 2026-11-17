package com.parkmate.app.system.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CompassReading(
    val azimuthDegrees: Float,
    val isSensorAvailable: Boolean
)

@Singleton
class CompassSensorMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    fun observeAzimuth(): Flow<CompassReading> = callbackFlow {
        val manager = sensorManager
        val rotationSensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        if (manager == null || rotationSensor == null) {
            trySend(CompassReading(azimuthDegrees = 0f, isSensorAvailable = false))
            awaitClose { }
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRadians = orientationAngles[0].toDouble()
                val azimuthDegrees = ((Math.toDegrees(azimuthRadians) + 360.0) % 360.0).toFloat()
                trySend(CompassReading(azimuthDegrees = azimuthDegrees, isSensorAvailable = true))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            manager.unregisterListener(listener)
        }
    }
}
