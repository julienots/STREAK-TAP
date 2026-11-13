package com.stepstreak.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.Calendar

/** Planification des rappels d'inactivité et du contrôle du soir (20 h). */
object AlarmScheduler {
    private const val REMINDER_INTERVAL_MS = 90 * 60 * 1000L
    private const val EVENING_HOUR = 20

    private fun reminderIntent(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun eveningIntent(c: Context) = PendingIntent.getBroadcast(
        c, 1, Intent(c, EveningReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(c: Context) {
        scheduleReminder(c)
        scheduleEvening(c)
    }

    fun scheduleReminder(c: Context) {
        if (!StepPrefs.remindersEnabled(c)) { am(c).cancel(reminderIntent(c)); return }
        try {
            am(c).setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + REMINDER_INTERVAL_MS,
                reminderIntent(c)
            )
        } catch (e: Exception) { }
    }

    fun cancelReminder(c: Context) = am(c).cancel(reminderIntent(c))

    /** Prochain 20 h (aujourd'hui ou demain). Toujours planifié : sert aussi au résumé hebdo. */
    fun scheduleEvening(c: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, EVENING_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 60_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        try {
            am(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, eveningIntent(c))
        } catch (e: Exception) { }
    }
}

/** Rappel "envie de marcher ?" si aucune progression depuis un moment (8 h – 22 h). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!StepPrefs.remindersEnabled(context)) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 8..21) {
            val goalReached = StepPrefs.getTodaySteps(context) >= StepPrefs.dailyGoal(context)
            val stagnant = StepPrefs.stepsStagnantSinceLastCheck(context)
            if (!goalReached && stagnant && StepPrefs.minutesSinceLastActivity(context) >= 90) {
                NotificationHelper.showWalkReminder(context)
            }
        }
        AlarmScheduler.scheduleReminder(context)
    }
}

/** 20 h : alerte "série en danger" + résumé de la semaine le dimanche. */
class EveningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (StepPrefs.eveningAlertEnabled(context) && StepPrefs.streakAtRisk(context)) {
            NotificationHelper.showStreakDanger(context)
        }
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            NotificationHelper.showWeeklySummary(context)
        }
        AlarmScheduler.scheduleEvening(context)
        StepService.start(context)
    }
}

/** Relance tout après un redémarrage du téléphone ou une mise à jour de l'app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                StepService.start(context)
                AlarmScheduler.scheduleAll(context)
                WidgetUpdater.updateAll(context)
            }
        }
    }
}
