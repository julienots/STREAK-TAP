package com.stepstreak.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

/** Grand widget 4×2 : anneau, pas, stats, flamme et mini-histogramme de la semaine. */
class StepWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
    }
}

/** Widget 2×2 : anneau de progression seul. */
class RingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
    }
}

/** Widget 2×2 : flamme de la série. */
class StreakWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
    }
}

object WidgetUpdater {

    private fun ids(c: Context, m: AppWidgetManager, cls: Class<*>): IntArray =
        try { m.getAppWidgetIds(ComponentName(c, cls)) } catch (e: Exception) { IntArray(0) }

    private fun openApp(c: Context, tab: Int): PendingIntent = PendingIntent.getActivity(
        c, 200 + tab,
        Intent(c, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun updateAll(c: Context) {
        try {
            val m = AppWidgetManager.getInstance(c) ?: return
            val big = ids(c, m, StepWidgetProvider::class.java)
            val ring = ids(c, m, RingWidgetProvider::class.java)
            val streakIds = ids(c, m, StreakWidgetProvider::class.java)
            if (big.isEmpty() && ring.isEmpty() && streakIds.isEmpty()) return

            val steps = StepPrefs.getTodaySteps(c)
            val goal = StepPrefs.dailyGoal(c)
            val streak = StepPrefs.getStreak(c)
            val ratio = if (goal > 0) steps.toFloat() / goal else 0f
            val percent = (ratio * 100).toInt()
            val density = c.resources.displayMetrics.density

            if (big.isNotEmpty()) {
                val days = StepPrefs.lastDays(c, 7)
                val letters = days.map { dayLetter(it.first) }
                val v = RemoteViews(c.packageName, R.layout.widget_step).apply {
                    setImageViewBitmap(R.id.wRing, BitmapRenderer.ring((110 * density).toInt(), ratio, 0x22FFFFFF, 0.11f, true))
                    setTextViewText(R.id.wPercent, "$percent%")
                    setTextViewText(R.id.wSteps, Fmt.int(steps))
                    setTextViewText(R.id.wGoal, "/ ${Fmt.int(goal)} pas")
                    setTextViewText(R.id.wKm, "📍 ${Fmt.km(Fitness.km(c, steps.toLong()))} km")
                    setTextViewText(R.id.wKcal, "🔥 ${Fitness.kcal(c, steps.toLong())} kcal")
                    setImageViewBitmap(R.id.wFlame, BitmapRenderer.flame((40 * density).toInt(), streak > 0))
                    setTextViewText(R.id.wStreak, streak.toString())
                    setImageViewBitmap(R.id.wWeek, BitmapRenderer.weekBars((260 * density).toInt(), (40 * density).toInt(),
                        days.map { it.second }, goal, letters))
                    setOnClickPendingIntent(R.id.wRoot, openApp(c, 0))
                }
                big.forEach { m.updateAppWidget(it, v) }
            }

            if (ring.isNotEmpty()) {
                val v = RemoteViews(c.packageName, R.layout.widget_ring).apply {
                    setImageViewBitmap(R.id.wRing, BitmapRenderer.ring((150 * density).toInt(), ratio, 0x22FFFFFF, 0.1f, true))
                    setTextViewText(R.id.wSteps, Fmt.int(steps))
                    setTextViewText(R.id.wPercent, if (ratio >= 1f) "✓ $percent%" else "$percent%")
                    setOnClickPendingIntent(R.id.wRoot, openApp(c, 0))
                }
                ring.forEach { m.updateAppWidget(it, v) }
            }

            if (streakIds.isNotEmpty()) {
                val grade = Grades.forStreak(streak)
                val atRisk = StepPrefs.streakAtRisk(c)
                val v = RemoteViews(c.packageName, R.layout.widget_streak).apply {
                    setImageViewBitmap(R.id.wFlame, BitmapRenderer.flame((72 * density).toInt(), streak > 0))
                    setTextViewText(R.id.wStreak, streak.toString())
                    setTextViewText(R.id.wLabel, if (streak > 1) "jours de série" else "jour de série")
                    setTextViewText(R.id.wGrade, when {
                        streak == 0 -> "Allume ta flamme !"
                        atRisk -> "⚠️ À valider aujourd'hui"
                        else -> "${grade.emoji} ${grade.name}"
                    })
                    setOnClickPendingIntent(R.id.wRoot, openApp(c, 2))
                }
                streakIds.forEach { m.updateAppWidget(it, v) }
            }
        } catch (e: Exception) { }
    }

    fun dayLetter(key: String): String {
        val d = StepPrefs.parseDay(key) ?: return ""
        val cal = Calendar.getInstance().apply { time = d }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "L"
            Calendar.TUESDAY -> "M"
            Calendar.WEDNESDAY -> "M"
            Calendar.THURSDAY -> "J"
            Calendar.FRIDAY -> "V"
            Calendar.SATURDAY -> "S"
            else -> "D"
        }
    }
}
