package com.stepstreak.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Stockage local de toutes les données de StepStreak (SharedPreferences) :
 * pas du jour, historique, série (streak), gels de série, records, réglages.
 */
object StepPrefs {
    private const val PREFS = "step_streak_prefs"

    private const val KEY_BASELINE = "baseline"
    private const val KEY_BASELINE_DATE = "baseline_date"
    private const val KEY_LAST_SENSOR = "last_sensor_value"
    private const val KEY_TODAY_STEPS = "today_steps"
    private const val KEY_TOTAL_STEPS = "total_steps"
    private const val KEY_HISTORY = "history_json"
    private const val KEY_STREAK = "streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_LAST_COUNTED_DATE = "last_counted_date"
    private const val KEY_GOAL_DAYS = "goal_days_count"
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_CELEBRATED_GOAL_DATE = "celebrated_goal_date"
    private const val KEY_LAST_ACTIVITY_TS = "last_activity_ts"
    private const val KEY_LAST_STEPS_AT_ACTIVITY_CHECK = "last_steps_at_activity_check"
    private const val KEY_SOUNDS_ENABLED = "sounds_enabled"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_EVENING_ENABLED = "evening_alert_enabled"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_MILESTONES_ENABLED = "milestones_enabled"
    private const val KEY_HEIGHT = "height_cm"
    private const val KEY_WEIGHT = "weight_kg"
    private const val KEY_FREEZES = "streak_freezes"
    private const val KEY_FROZEN_DAYS = "frozen_days"
    private const val KEY_BEST_DAY_STEPS = "best_day_steps"
    private const val KEY_BEST_DAY_DATE = "best_day_date"
    private const val KEY_MILESTONE_DATE = "milestone_date"
    private const val KEY_MILESTONE_LEVEL = "milestone_level"
    private const val KEY_CONFETTI_DATE = "confetti_date"
    private const val KEY_CHALLENGE_DATE = "challenge_date"
    private const val KEY_CHALLENGE_INDEX = "challenge_index"
    private const val KEY_CHALLENGE_TARGET = "challenge_target"
    private const val KEY_CHALLENGE_DONE_DATE = "challenge_done_date"
    private const val KEY_CHALLENGES_DONE = "challenges_done_count"

    const val MAX_FREEZES = 2
    const val FREEZE_EVERY_DAYS = 7

    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- dates

    fun dayKey(date: Date): String = fmt().format(date)
    fun todayKey(): String = dayKey(Date())

    fun dayKeyOffset(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return dayKey(cal.time)
    }

    fun parseDay(key: String): Date? = try { fmt().parse(key) } catch (e: Exception) { null }

    /** Nombre de jours entre deux clés de date (b - a). */
    fun daysBetween(a: String, b: String): Int {
        val da = parseDay(a) ?: return Int.MAX_VALUE
        val db = parseDay(b) ?: return Int.MAX_VALUE
        return Math.round((db.time - da.time) / 86_400_000.0).toInt()
    }

    // ------------------------------------------------------------- réglages

    fun dailyGoal(context: Context): Int = prefs(context).getInt(KEY_DAILY_GOAL, 8000)
    fun setDailyGoal(context: Context, goal: Int) {
        prefs(context).edit().putInt(KEY_DAILY_GOAL, goal.coerceIn(500, 100_000)).apply()
    }

    fun heightCm(context: Context): Int = prefs(context).getInt(KEY_HEIGHT, 170)
    fun setHeightCm(context: Context, v: Int) {
        prefs(context).edit().putInt(KEY_HEIGHT, v.coerceIn(100, 230)).apply()
    }

    fun weightKg(context: Context): Int = prefs(context).getInt(KEY_WEIGHT, 70)
    fun setWeightKg(context: Context, v: Int) {
        prefs(context).edit().putInt(KEY_WEIGHT, v.coerceIn(30, 250)).apply()
    }

    fun soundsEnabled(c: Context) = prefs(c).getBoolean(KEY_SOUNDS_ENABLED, true)
    fun setSoundsEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_SOUNDS_ENABLED, v).apply()

    fun remindersEnabled(c: Context) = prefs(c).getBoolean(KEY_REMINDERS_ENABLED, true)
    fun setRemindersEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_REMINDERS_ENABLED, v).apply()

    fun eveningAlertEnabled(c: Context) = prefs(c).getBoolean(KEY_EVENING_ENABLED, true)
    fun setEveningAlertEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_EVENING_ENABLED, v).apply()

    fun hapticsEnabled(c: Context) = prefs(c).getBoolean(KEY_HAPTICS_ENABLED, true)
    fun setHapticsEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_HAPTICS_ENABLED, v).apply()

    fun milestonesEnabled(c: Context) = prefs(c).getBoolean(KEY_MILESTONES_ENABLED, true)
    fun setMilestonesEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_MILESTONES_ENABLED, v).apply()

    fun shouldShowConfettiToday(c: Context): Boolean = prefs(c).getString(KEY_CONFETTI_DATE, "") != todayKey()
    fun markConfettiShown(c: Context) = prefs(c).edit().putString(KEY_CONFETTI_DATE, todayKey()).apply()

    // ------------------------------------------------------------- lecture

    fun getTodaySteps(c: Context): Int {
        val p = prefs(c)
        // Si la journée a changé sans nouvelle lecture du capteur, on affiche 0 pour aujourd'hui.
        return if (p.getString(KEY_BASELINE_DATE, null) == todayKey()) p.getInt(KEY_TODAY_STEPS, 0) else 0
    }

    fun getTotalSteps(c: Context): Long = prefs(c).getLong(KEY_TOTAL_STEPS_L, legacyTotal(c)) + getTodaySteps(c)
    private const val KEY_TOTAL_STEPS_L = "total_steps_long"
    private fun legacyTotal(c: Context): Long = prefs(c).getInt(KEY_TOTAL_STEPS, 0).toLong()

    fun getStreak(c: Context): Int = prefs(c).getInt(KEY_STREAK, 0)
    fun getLongestStreak(c: Context): Int = prefs(c).getInt(KEY_LONGEST_STREAK, 0)
    fun getGoalDays(c: Context): Int = prefs(c).getInt(KEY_GOAL_DAYS, 0)
    fun getFreezes(c: Context): Int = prefs(c).getInt(KEY_FREEZES, 0)
    fun getChallengesDone(c: Context): Int = prefs(c).getInt(KEY_CHALLENGES_DONE, 0)

    fun getFrozenDays(c: Context): Set<String> =
        prefs(c).getStringSet(KEY_FROZEN_DAYS, emptySet()) ?: emptySet()

    fun getBestDay(c: Context): Pair<Int, String?> {
        val p = prefs(c)
        val best = p.getInt(KEY_BEST_DAY_STEPS, 0)
        val today = getTodaySteps(c)
        return if (today > best) today to todayKey() else best to p.getString(KEY_BEST_DAY_DATE, null)
    }

    /** Le jour a-t-il été validé (objectif atteint) aujourd'hui ? */
    fun goalReachedToday(c: Context): Boolean = prefs(c).getString(KEY_LAST_COUNTED_DATE, "") == todayKey()

    /** Historique jour -> pas (aujourd'hui inclus). */
    fun history(c: Context): Map<String, Int> {
        val result = HashMap<String, Int>()
        try {
            val json = JSONObject(prefs(c).getString(KEY_HISTORY, "{}") ?: "{}")
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = json.optInt(k, 0)
            }
        } catch (e: Exception) { /* historique corrompu : on repart de zéro */ }
        result[todayKey()] = getTodaySteps(c)
        return result
    }

    /** Les n derniers jours, du plus ancien au plus récent (aujourd'hui en dernier). */
    fun lastDays(c: Context, n: Int, history: Map<String, Int> = this.history(c)): List<Pair<String, Int>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
        val out = ArrayList<Pair<String, Int>>(n)
        repeat(n) {
            val k = dayKey(cal.time)
            out.add(k to (history[k] ?: 0))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return out
    }

    private fun saveHistoryEntry(p: SharedPreferences, date: String, steps: Int) {
        try {
            val json = JSONObject(p.getString(KEY_HISTORY, "{}") ?: "{}")
            json.put(date, steps)
            if (json.length() > 450) {
                val keys = ArrayList<String>()
                val it = json.keys()
                while (it.hasNext()) keys.add(it.next())
                keys.sort()
                keys.take(json.length() - 450).forEach { json.remove(it) }
            }
            p.edit().putString(KEY_HISTORY, json.toString()).apply()
        } catch (e: Exception) { }
    }

    // ------------------------------------------------------------- capteur

    /** Ce que le service doit célébrer suite à une lecture du capteur. */
    data class SensorResult(
        val todaySteps: Int,
        val goalJustReached: Boolean = false,
        val milestoneCrossed: Int? = null,
        val freezeEarned: Boolean = false,
        val challengeCompleted: DailyChallenge.Challenge? = null,
        val newAchievements: List<Achievement> = emptyList(),
        val gradeUp: Grade? = null
    )

    /** Appelé à chaque lecture du capteur (valeur cumulée depuis le dernier redémarrage). */
    fun onSensorReading(context: Context, sensorTotal: Int): SensorResult {
        val p = prefs(context)
        val today = todayKey()
        val baselineDate = p.getString(KEY_BASELINE_DATE, null)
        val lastSensor = p.getInt(KEY_LAST_SENSOR, -1)

        if (baselineDate != today) {
            if (baselineDate != null) finalizeDay(context, baselineDate, p.getInt(KEY_TODAY_STEPS, 0))
            p.edit()
                .putString(KEY_BASELINE_DATE, today)
                .putInt(KEY_BASELINE, sensorTotal)
                .putInt(KEY_LAST_SENSOR, sensorTotal)
                .putInt(KEY_TODAY_STEPS, 0)
                .putLong(KEY_LAST_ACTIVITY_TS, System.currentTimeMillis())
                .putInt(KEY_LAST_STEPS_AT_ACTIVITY_CHECK, -1)
                .apply()
            checkMissedStreak(context)
            return SensorResult(0)
        }

        var baseline = p.getInt(KEY_BASELINE, sensorTotal)
        val previousSteps = p.getInt(KEY_TODAY_STEPS, 0)
        if (lastSensor > sensorTotal) {
            // Le téléphone a redémarré : le capteur repart de 0, on conserve les pas déjà faits.
            baseline = sensorTotal - previousSteps
        }
        val steps = (sensorTotal - baseline).coerceAtLeast(0)
        val edit = p.edit()
            .putInt(KEY_BASELINE, baseline)
            .putInt(KEY_LAST_SENSOR, sensorTotal)
            .putInt(KEY_TODAY_STEPS, steps)
        if (steps > previousSteps) edit.putLong(KEY_LAST_ACTIVITY_TS, System.currentTimeMillis())
        edit.apply()

        if (steps == previousSteps) return SensorResult(steps)
        return evaluateToday(context, steps)
    }

    private fun evaluateToday(context: Context, steps: Int): SensorResult {
        val p = prefs(context)
        val today = todayKey()
        val goal = dailyGoal(context)

        val gradeBefore = Grades.forStreak(getStreak(context))
        var freezeEarned = false
        var goalJustReached = false
        if (steps >= goal && p.getString(KEY_LAST_COUNTED_DATE, "") != today) {
            checkMissedStreak(context)
            freezeEarned = incrementStreak(context, today)
        }
        if (steps >= goal && p.getString(KEY_CELEBRATED_GOAL_DATE, "") != today) {
            p.edit().putString(KEY_CELEBRATED_GOAL_DATE, today).apply()
            goalJustReached = true
        }
        val gradeAfter = Grades.forStreak(getStreak(context))

        if (steps > p.getInt(KEY_BEST_DAY_STEPS, 0)) {
            p.edit().putInt(KEY_BEST_DAY_STEPS, steps).putString(KEY_BEST_DAY_DATE, today).apply()
        }

        // Paliers 50 % / 75 % (une seule notif par palier et par jour)
        var milestone: Int? = null
        if (!goalJustReached && goal > 0) {
            val ratio = steps.toFloat() / goal
            val level = when {
                ratio >= 1f -> 100
                ratio >= 0.75f -> 75
                ratio >= 0.5f -> 50
                else -> 0
            }
            val storedLevel = if (p.getString(KEY_MILESTONE_DATE, "") == today) p.getInt(KEY_MILESTONE_LEVEL, 0) else 0
            if (level > storedLevel) {
                p.edit().putString(KEY_MILESTONE_DATE, today).putInt(KEY_MILESTONE_LEVEL, level).apply()
                if (level in 50..75) milestone = level
            }
        }

        val challenge = DailyChallenge.checkCompleted(context, steps)
        val achievements = Achievements.checkNew(context)

        return SensorResult(
            todaySteps = steps,
            goalJustReached = goalJustReached,
            milestoneCrossed = milestone,
            freezeEarned = freezeEarned,
            challengeCompleted = challenge,
            newAchievements = achievements,
            gradeUp = if (gradeAfter.minStreak > gradeBefore.minStreak) gradeAfter else null
        )
    }

    /** Clôture d'une journée passée : historique + total + série. */
    private fun finalizeDay(context: Context, date: String, steps: Int) {
        val p = prefs(context)
        saveHistoryEntry(p, date, steps)
        p.edit().putLong(KEY_TOTAL_STEPS_L, prefs(context).getLong(KEY_TOTAL_STEPS_L, legacyTotal(context)) + steps).apply()
        if (steps >= dailyGoal(context) && p.getString(KEY_LAST_COUNTED_DATE, "") != date) {
            incrementStreak(context, date)
        }
        if (steps > p.getInt(KEY_BEST_DAY_STEPS, 0)) {
            p.edit().putInt(KEY_BEST_DAY_STEPS, steps).putString(KEY_BEST_DAY_DATE, date).apply()
        }
    }

    /** Valide une journée : incrémente la série. Retourne true si un gel de série vient d'être gagné. */
    private fun incrementStreak(context: Context, countedDate: String): Boolean {
        val p = prefs(context)
        val lastCounted = p.getString(KEY_LAST_COUNTED_DATE, null)
        if (lastCounted == countedDate) return false
        val current = p.getInt(KEY_STREAK, 0)
        val newStreak = if (lastCounted != null && daysBetween(lastCounted, countedDate) == 1) current + 1 else 1
        val longest = maxOf(newStreak, p.getInt(KEY_LONGEST_STREAK, 0))
        var freezes = p.getInt(KEY_FREEZES, 0)
        var earned = false
        if (newStreak % FREEZE_EVERY_DAYS == 0 && freezes < MAX_FREEZES) {
            freezes++
            earned = true
        }
        p.edit()
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_LONGEST_STREAK, longest)
            .putString(KEY_LAST_COUNTED_DATE, countedDate)
            .putInt(KEY_GOAL_DAYS, p.getInt(KEY_GOAL_DAYS, 0) + 1)
            .putInt(KEY_FREEZES, freezes)
            .apply()
        return earned
    }

    /**
     * Vérifie si des jours ont été manqués. Si l'utilisateur possède assez de gels de série,
     * ils sont consommés automatiquement et la série est sauvée. Sinon la série retombe à 0.
     * Retourne le nombre de gels utilisés.
     */
    fun checkMissedStreak(context: Context): Int {
        val p = prefs(context)
        val last = p.getString(KEY_LAST_COUNTED_DATE, null) ?: return 0
        val today = todayKey()
        val gap = daysBetween(last, today)
        if (gap <= 1) return 0
        val missed = gap - 1
        val freezes = p.getInt(KEY_FREEZES, 0)
        val streak = p.getInt(KEY_STREAK, 0)
        if (streak > 0 && missed <= freezes) {
            val frozen = getFrozenDays(context).toMutableSet()
            for (i in 1..missed) {
                val cal = Calendar.getInstance()
                cal.time = parseDay(last) ?: Date()
                cal.add(Calendar.DAY_OF_YEAR, i)
                frozen.add(dayKey(cal.time))
            }
            p.edit()
                .putInt(KEY_FREEZES, freezes - missed)
                .putString(KEY_LAST_COUNTED_DATE, dayKeyOffset(-1))
                .putStringSet(KEY_FROZEN_DAYS, frozen)
                .apply()
            return missed
        }
        if (streak != 0) p.edit().putInt(KEY_STREAK, 0).apply()
        return 0
    }

    /** La série est-elle en danger ce soir (objectif pas encore atteint aujourd'hui) ? */
    fun streakAtRisk(c: Context): Boolean = getStreak(c) > 0 && !goalReachedToday(c)

    // ------------------------------------------------------- défi du jour

    fun challengeState(c: Context): Triple<String?, Int, Int> {
        val p = prefs(c)
        return Triple(p.getString(KEY_CHALLENGE_DATE, null), p.getInt(KEY_CHALLENGE_INDEX, 0), p.getInt(KEY_CHALLENGE_TARGET, 0))
    }

    fun saveChallenge(c: Context, date: String, index: Int, target: Int) {
        prefs(c).edit().putString(KEY_CHALLENGE_DATE, date).putInt(KEY_CHALLENGE_INDEX, index)
            .putInt(KEY_CHALLENGE_TARGET, target).apply()
    }

    fun challengeDoneToday(c: Context): Boolean = prefs(c).getString(KEY_CHALLENGE_DONE_DATE, "") == todayKey()

    fun markChallengeDone(c: Context) {
        val p = prefs(c)
        p.edit().putString(KEY_CHALLENGE_DONE_DATE, todayKey())
            .putInt(KEY_CHALLENGES_DONE, p.getInt(KEY_CHALLENGES_DONE, 0) + 1).apply()
    }

    // ------------------------------------------- suivi d'inactivité (rappels)

    fun minutesSinceLastActivity(c: Context): Long {
        val last = prefs(c).getLong(KEY_LAST_ACTIVITY_TS, 0L)
        if (last == 0L) return 0L
        return (System.currentTimeMillis() - last) / 60_000L
    }

    fun stepsStagnantSinceLastCheck(c: Context): Boolean {
        val p = prefs(c)
        val current = getTodaySteps(c)
        val previous = p.getInt(KEY_LAST_STEPS_AT_ACTIVITY_CHECK, -1)
        p.edit().putInt(KEY_LAST_STEPS_AT_ACTIVITY_CHECK, current).apply()
        return previous == current
    }
}
