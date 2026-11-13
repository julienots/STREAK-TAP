package com.stepstreak.app

import android.content.Context
import kotlin.math.roundToInt
import kotlin.random.Random

// ======================================================================== Grades

data class Grade(
    val name: String,
    val emoji: String,
    val minStreak: Int,
    val colorStart: Int,
    val colorEnd: Int
)

object Grades {
    val all = listOf(
        Grade("Bronze", "🥉", 0, 0xFFE0A070.toInt(), 0xFF8A4F24.toInt()),
        Grade("Argent", "🥈", 3, 0xFFEEF1F6.toInt(), 0xFF8E97A6.toInt()),
        Grade("Or", "🥇", 7, 0xFFFFE27A.toInt(), 0xFFE09A00.toInt()),
        Grade("Platine", "💠", 14, 0xFFB8F3FF.toInt(), 0xFF4FA8C9.toInt()),
        Grade("Diamant", "💎", 30, 0xFF8FD3FF.toInt(), 0xFF6C63FF.toInt()),
        Grade("Maître", "🏆", 60, 0xFFFF9BD2.toInt(), 0xFF9B5CFF.toInt()),
        Grade("Légende", "👑", 100, 0xFFFFD24D.toInt(), 0xFFFF5E3A.toInt()),
        Grade("Mythique", "🌌", 200, 0xFF7CFFCB.toInt(), 0xFF7C5CFF.toInt())
    )

    fun forStreak(streak: Int): Grade = all.last { streak >= it.minStreak }
    fun next(streak: Int): Grade? = all.firstOrNull { it.minStreak > streak }
    fun index(grade: Grade): Int = all.indexOf(grade)
}

// ====================================================================== Fitness

object Fitness {
    fun strideMeters(c: Context): Double = StepPrefs.heightCm(c) * 0.415 / 100.0
    fun km(c: Context, steps: Long): Double = steps * strideMeters(c) / 1000.0
    fun kcal(c: Context, steps: Long): Int = (steps * StepPrefs.weightKg(c) * 0.00057).roundToInt()
    fun activeMinutes(steps: Long): Int = (steps / 100).toInt()
}

// ================================================================= Succès

enum class AchCategory(val label: String, val emoji: String) {
    STREAK("Séries", "🔥"),
    DAILY("Records du jour", "⚡"),
    TOTAL("Cumul de pas", "👣"),
    DISTANCE("Distance parcourue", "🗺️"),
    SPECIAL("Spéciaux", "✨")
}

enum class Metric { LONGEST_STREAK, BEST_DAY, TOTAL_STEPS, TOTAL_KM, GOAL_DAYS, CHALLENGES, FREEZE_USED }

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val category: AchCategory,
    val metric: Metric,
    val target: Double
)

object Achievements {
    private const val KEY_UNLOCKED = "achievements_unlocked"

    val all: List<Achievement> = listOf(
        Achievement("streak_3", "🌱", "Ça démarre", "3 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 3.0),
        Achievement("streak_7", "🔥", "Semaine en feu", "7 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 7.0),
        Achievement("streak_14", "☄️", "Inarrêtable", "14 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 14.0),
        Achievement("streak_30", "🌙", "Un mois entier", "30 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 30.0),
        Achievement("streak_60", "🏆", "Discipline d'acier", "60 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 60.0),
        Achievement("streak_100", "💯", "Centurion", "100 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 100.0),
        Achievement("streak_200", "🌋", "Volcan", "200 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 200.0),
        Achievement("streak_365", "👑", "Une année parfaite", "365 jours de série", AchCategory.STREAK, Metric.LONGEST_STREAK, 365.0),

        Achievement("day_10k", "⚡", "Club des 10 000", "10 000 pas en une journée", AchCategory.DAILY, Metric.BEST_DAY, 10_000.0),
        Achievement("day_15k", "🚀", "Turbo", "15 000 pas en une journée", AchCategory.DAILY, Metric.BEST_DAY, 15_000.0),
        Achievement("day_20k", "🦅", "Envol", "20 000 pas en une journée", AchCategory.DAILY, Metric.BEST_DAY, 20_000.0),
        Achievement("day_30k", "🦄", "Hors norme", "30 000 pas en une journée", AchCategory.DAILY, Metric.BEST_DAY, 30_000.0),

        Achievement("total_100k", "👟", "Bien chaussé", "100 000 pas au total", AchCategory.TOTAL, Metric.TOTAL_STEPS, 100_000.0),
        Achievement("total_500k", "🥾", "Randonneur", "500 000 pas au total", AchCategory.TOTAL, Metric.TOTAL_STEPS, 500_000.0),
        Achievement("total_1m", "🎖️", "Millionnaire", "1 million de pas", AchCategory.TOTAL, Metric.TOTAL_STEPS, 1_000_000.0),
        Achievement("total_5m", "🪐", "Galactique", "5 millions de pas", AchCategory.TOTAL, Metric.TOTAL_STEPS, 5_000_000.0),

        Achievement("km_10", "📍", "Premiers kilomètres", "10 km parcourus", AchCategory.DISTANCE, Metric.TOTAL_KM, 10.0),
        Achievement("km_42", "🏅", "Marathonien", "42,2 km parcourus", AchCategory.DISTANCE, Metric.TOTAL_KM, 42.195),
        Achievement("km_100", "🗻", "Cent bornes", "100 km parcourus", AchCategory.DISTANCE, Metric.TOTAL_KM, 100.0),
        Achievement("km_500", "🧭", "Explorateur", "500 km parcourus", AchCategory.DISTANCE, Metric.TOTAL_KM, 500.0),
        Achievement("km_1000", "🌍", "Tour du pays", "1 000 km parcourus", AchCategory.DISTANCE, Metric.TOTAL_KM, 1000.0),

        Achievement("goal_1", "🎯", "Dans le mille", "Atteindre l'objectif une 1re fois", AchCategory.SPECIAL, Metric.GOAL_DAYS, 1.0),
        Achievement("goal_50", "🎪", "Habitué", "Objectif atteint 50 jours", AchCategory.SPECIAL, Metric.GOAL_DAYS, 50.0),
        Achievement("chal_1", "🧩", "Challenger", "Réussir un défi du jour", AchCategory.SPECIAL, Metric.CHALLENGES, 1.0),
        Achievement("chal_10", "🃏", "Maître des défis", "Réussir 10 défis du jour", AchCategory.SPECIAL, Metric.CHALLENGES, 10.0),
        Achievement("freeze_1", "🧊", "Sauvé par le gel", "Une série sauvée par un gel", AchCategory.SPECIAL, Metric.FREEZE_USED, 1.0)
    )

    data class Snapshot(
        val longest: Int,
        val bestDay: Int,
        val totalSteps: Long,
        val totalKm: Double,
        val goalDays: Int,
        val challenges: Int,
        val freezeUsed: Int
    )

    fun snapshot(c: Context): Snapshot {
        val total = StepPrefs.getTotalSteps(c)
        return Snapshot(
            longest = StepPrefs.getLongestStreak(c),
            bestDay = StepPrefs.getBestDay(c).first,
            totalSteps = total,
            totalKm = Fitness.km(c, total),
            goalDays = StepPrefs.getGoalDays(c),
            challenges = StepPrefs.getChallengesDone(c),
            freezeUsed = StepPrefs.getFrozenDays(c).size
        )
    }

    fun value(a: Achievement, s: Snapshot): Double = when (a.metric) {
        Metric.LONGEST_STREAK -> s.longest.toDouble()
        Metric.BEST_DAY -> s.bestDay.toDouble()
        Metric.TOTAL_STEPS -> s.totalSteps.toDouble()
        Metric.TOTAL_KM -> s.totalKm
        Metric.GOAL_DAYS -> s.goalDays.toDouble()
        Metric.CHALLENGES -> s.challenges.toDouble()
        Metric.FREEZE_USED -> s.freezeUsed.toDouble()
    }

    fun progress(a: Achievement, s: Snapshot): Float = (value(a, s) / a.target).toFloat().coerceIn(0f, 1f)
    fun isUnlocked(a: Achievement, s: Snapshot): Boolean = value(a, s) >= a.target

    fun unlockedCount(c: Context): Int {
        val s = snapshot(c)
        return all.count { isUnlocked(it, s) }
    }

    /** Détecte les succès nouvellement débloqués (et les mémorise). */
    fun checkNew(c: Context): List<Achievement> {
        val p = StepPrefs.prefs(c)
        val s = snapshot(c)
        val nowUnlocked = all.filter { isUnlocked(it, s) }.map { it.id }.toSet()
        if (!p.contains(KEY_UNLOCKED)) {
            // Première exécution (ou mise à jour) : on initialise sans notifier.
            p.edit().putStringSet(KEY_UNLOCKED, nowUnlocked).apply()
            return emptyList()
        }
        val stored = p.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()
        val fresh = nowUnlocked - stored
        if (fresh.isEmpty()) return emptyList()
        p.edit().putStringSet(KEY_UNLOCKED, stored + fresh).apply()
        return all.filter { it.id in fresh }
    }

    fun formatValue(a: Achievement, v: Double): String = when (a.metric) {
        Metric.TOTAL_KM -> Fmt.km(v)
        else -> Fmt.long(v.toLong())
    }
}

// ============================================================ Niveau / XP

object Levels {
    data class LevelInfo(val level: Int, val xpInLevel: Long, val xpForNext: Long, val totalXp: Long)

    fun totalXp(c: Context): Long =
        StepPrefs.getTotalSteps(c) / 10 +
            StepPrefs.getGoalDays(c) * 100L +
            StepPrefs.getLongestStreak(c) * 30L +
            StepPrefs.getChallengesDone(c) * 150L +
            Achievements.unlockedCount(c) * 250L

    fun info(c: Context): LevelInfo {
        var remaining = totalXp(c)
        val total = remaining
        var level = 1
        while (true) {
            val need = 800L + (level - 1) * 300L
            if (remaining < need) return LevelInfo(level, remaining, need, total)
            remaining -= need
            level++
        }
    }

    fun title(level: Int): String = when {
        level >= 40 -> "Titan de la marche"
        level >= 30 -> "Légende urbaine"
        level >= 20 -> "Grand voyageur"
        level >= 12 -> "Arpenteur"
        level >= 6 -> "Marcheur confirmé"
        level >= 3 -> "Promeneur"
        else -> "Débutant"
    }
}

// ============================================================= Défi du jour

object DailyChallenge {
    data class Challenge(val emoji: String, val title: String, val desc: String, val target: Int, val xp: Int)

    private fun round250(v: Double): Int = ((v / 250.0).roundToInt() * 250).coerceAtLeast(500)

    private fun build(index: Int, target: Int): Challenge = when (index) {
        0 -> Challenge("🎯", "Pile dans la cible", "Atteins ton objectif du jour", target, 100)
        1 -> Challenge("🚀", "Dépassement", "Fais 20 % de plus que ton objectif", target, 200)
        2 -> Challenge("🔥", "Mode turbo", "Fais une fois et demie ton objectif", target, 350)
        3 -> Challenge("🌟", "Record de la semaine", "Bats ton meilleur jour des 7 derniers jours", target, 300)
        4 -> Challenge("⚡", "Club des 10 000", "Atteins 10 000 pas aujourd'hui", target, 250)
        else -> Challenge("🏔️", "Au-dessus de la moyenne", "Dépasse ta moyenne de la semaine de 15 %", target, 200)
    }

    /** Défi du jour, déterminé une fois par jour (stable toute la journée). */
    fun today(c: Context): Challenge {
        val today = StepPrefs.todayKey()
        val (date, idx, target) = StepPrefs.challengeState(c)
        if (date == today && target > 0) return build(idx, target)

        val goal = StepPrefs.dailyGoal(c)
        val last7 = StepPrefs.lastDays(c, 8).dropLast(1).map { it.second }
        val best7 = last7.maxOrNull() ?: 0
        val avg7 = if (last7.isNotEmpty()) last7.average() else 0.0
        val index = Random(today.hashCode()).nextInt(6)
        val t = when (index) {
            0 -> goal
            1 -> round250(goal * 1.2)
            2 -> round250(goal * 1.5)
            3 -> maxOf(best7 + 1, goal)
            4 -> maxOf(10_000, goal)
            else -> maxOf(round250(avg7 * 1.15), goal)
        }
        StepPrefs.saveChallenge(c, today, index, t)
        return build(index, t)
    }

    /** Retourne le défi s'il vient tout juste d'être réussi. */
    fun checkCompleted(c: Context, steps: Int): Challenge? {
        if (StepPrefs.challengeDoneToday(c)) return null
        val ch = today(c)
        if (steps >= ch.target) {
            StepPrefs.markChallengeDone(c)
            return ch
        }
        return null
    }
}

// ========================================================= Messages motivants

object Motivation {
    fun forProgress(steps: Int, goal: Int, streak: Int): String {
        if (goal <= 0) return "Continue comme ça !"
        val ratio = steps.toFloat() / goal
        val left = Fmt.int((goal - steps).coerceAtLeast(0))
        return when {
            ratio >= 2f -> "Double objectif ! Tu es une machine 🤖"
            ratio >= 1.5f -> "Tu exploses les compteurs 🚀"
            ratio >= 1f -> "Objectif atteint, bravo ! 🎉"
            ratio >= 0.9f -> "Dernière ligne droite : $left pas 🏁"
            ratio >= 0.75f -> "Plus que $left pas, tu y es presque 💪"
            ratio >= 0.5f -> "Mi-parcours passé, garde le rythme ⚡"
            ratio >= 0.25f -> "Bon début, continue sur ta lancée 🚶"
            steps == 0 && streak > 0 -> "Ta série de ${Fmt.days(streak)} t'attend 🔥"
            steps == 0 -> "Prêt·e pour une nouvelle journée ? 👟"
            else -> "Chaque pas compte, en route !"
        }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        in 18..22 -> "Bonsoir"
        else -> "Bonne nuit"
    }
}
