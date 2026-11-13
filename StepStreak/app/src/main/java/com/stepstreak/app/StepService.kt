package com.stepstreak.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock

/** Service en premier plan : écoute le podomètre et déclenche les célébrations. */
class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastNotifUpdate = 0L
    private var lastWidgetUpdate = 0L
    private var lastWidgetSteps = -1

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        val notif = NotificationHelper.buildTrackingNotification(this, StepPrefs.getTodaySteps(this))
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NotificationHelper.NOTIF_TRACKING_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NotificationHelper.NOTIF_TRACKING_ID, notif)
            }
        } catch (e: Exception) {
            // Permission manquante (reconnaissance d'activité) : on ne peut pas tourner en premier plan.
            stopSelf()
            return
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        val used = StepPrefs.checkMissedStreak(this)
        if (used > 0) NotificationHelper.showFreezeUsed(this, used)
        WidgetUpdater.updateAll(this)
        AlarmScheduler.scheduleAll(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val result = StepPrefs.onSensorReading(this, event.values[0].toInt())
        val now = SystemClock.elapsedRealtime()
        val important = result.goalJustReached || result.milestoneCrossed != null ||
            result.newAchievements.isNotEmpty() || result.challengeCompleted != null

        if (important || now - lastNotifUpdate > 4_000) {
            NotificationHelper.updateTrackingNotification(this, result.todaySteps)
            lastNotifUpdate = now
        }
        if (important || now - lastWidgetUpdate > 20_000 || kotlin.math.abs(result.todaySteps - lastWidgetSteps) >= 100) {
            WidgetUpdater.updateAll(this)
            lastWidgetUpdate = now
            lastWidgetSteps = result.todaySteps
        }
        sendBroadcast(
            Intent(ACTION_STEPS_UPDATED).setPackage(packageName)
                .putExtra(EXTRA_GOAL_REACHED, result.goalJustReached)
        )

        if (result.goalJustReached) NotificationHelper.showGoalReached(this, result.todaySteps)
        result.milestoneCrossed?.let { NotificationHelper.showMilestone(this, it, result.todaySteps) }
        result.gradeUp?.let { NotificationHelper.showGradeUp(this, it) }
        if (result.freezeEarned) NotificationHelper.showFreezeEarned(this)
        result.challengeCompleted?.let { NotificationHelper.showChallengeDone(this, it) }
        result.newAchievements.take(3).forEach { NotificationHelper.showAchievement(this, it) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STEPS_UPDATED = "com.stepstreak.app.STEPS_UPDATED"
        const val EXTRA_GOAL_REACHED = "goal_reached"

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, StepService::class.java))
            } catch (e: Exception) {
                // Démarrage refusé en arrière-plan : il sera relancé à la prochaine ouverture.
            }
        }
    }
}
