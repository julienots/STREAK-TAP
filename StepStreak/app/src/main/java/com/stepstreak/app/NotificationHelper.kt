package com.stepstreak.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

/**
 * Toutes les notifications de StepStreak :
 * - suivi permanent avec mise en page personnalisée (anneau, flamme, stats)
 * - célébrations illustrées (objectif, succès, grade, défi, gel de série)
 * - paliers 50 % / 75 %, série en danger, résumé hebdo, rappels d'inactivité
 */
object NotificationHelper {

    const val CHANNEL_TRACKING = "ss2_tracking"
    const val CHANNEL_CELEBRATION = "ss2_celebration"
    const val CHANNEL_PROGRESS = "ss2_progress"
    const val CHANNEL_DANGER = "ss2_danger"
    const val CHANNEL_REMINDER = "ss2_reminder"
    const val CHANNEL_SUMMARY = "ss2_summary"

    const val NOTIF_TRACKING_ID = 1001
    private const val NOTIF_GOAL_ID = 1002
    private const val NOTIF_REMINDER_ID = 1003
    private const val NOTIF_PROGRESS_ID = 1004
    private const val NOTIF_DANGER_ID = 1005
    private const val NOTIF_SUMMARY_ID = 1006
    private const val NOTIF_FREEZE_ID = 1007
    private const val NOTIF_CHALLENGE_ID = 1008
    private const val NOTIF_GRADE_ID = 1009
    private const val NOTIF_ACH_BASE = 2000

    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    private fun sound(c: Context, res: Int): Uri = Uri.parse("android.resource://${c.packageName}/$res")

    private val audioAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun ensureChannels(c: Context) {
        val m = nm(c)
        // Anciens canaux de la v1, remplacés
        listOf("step_streak_tracking", "step_streak_celebration", "step_streak_reminder").forEach {
            try { m.deleteNotificationChannel(it) } catch (e: Exception) { }
        }
        val channels = listOf(
            NotificationChannel(CHANNEL_TRACKING, "Suivi en direct", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ta progression du jour en temps réel"
                setShowBadge(false)
            },
            NotificationChannel(CHANNEL_CELEBRATION, "Célébrations 🎉", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Objectif atteint, succès, grades et défis réussis"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 80, 60, 80, 60, 220)
                setSound(sound(c, R.raw.chime_success), audioAttrs)
            },
            NotificationChannel(CHANNEL_PROGRESS, "Paliers de progression", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Quand tu passes 50 % et 75 % de ton objectif"
                setSound(sound(c, R.raw.chime_badge), audioAttrs)
            },
            NotificationChannel(CHANNEL_DANGER, "Série en danger 🔥", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerte du soir si ta série risque de s'éteindre"
                enableVibration(true)
                setSound(sound(c, R.raw.chime_nudge), audioAttrs)
            },
            NotificationChannel(CHANNEL_REMINDER, "Envie de marcher ?", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Petits rappels si tu n'as pas bougé depuis un moment"
                setSound(sound(c, R.raw.chime_nudge), audioAttrs)
            },
            NotificationChannel(CHANNEL_SUMMARY, "Résumé de la semaine", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ton bilan chaque dimanche soir"
            }
        )
        m.createNotificationChannels(channels)
    }

    private fun openApp(c: Context, tab: Int = 0): PendingIntent {
        val intent = Intent(c, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(c, 100 + tab, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // ----------------------------------------------------------- suivi live

    fun buildTrackingNotification(c: Context, steps: Int): Notification {
        val goal = StepPrefs.dailyGoal(c)
        val streak = StepPrefs.getStreak(c)
        val ratio = if (goal > 0) steps.toFloat() / goal else 0f
        val percent = (ratio * 100).toInt()
        val motivation = Motivation.forProgress(steps, goal, streak)
        val km = Fitness.km(c, steps.toLong())
        val kcal = Fitness.kcal(c, steps.toLong())
        val minutes = Fitness.activeMinutes(steps.toLong())
        val density = c.resources.displayMetrics.density

        val ringSmall = BitmapRenderer.ring((44 * density).toInt(), ratio, 0x33888888, 0.13f, false)
        val ringBig = BitmapRenderer.ring((92 * density).toInt(), ratio, 0x33888888, 0.1f, true)
        val flame = BitmapRenderer.flame((30 * density).toInt(), streak > 0)

        val small = RemoteViews(c.packageName, R.layout.notif_tracking_small).apply {
            setImageViewBitmap(R.id.nRing, ringSmall)
            setTextViewText(R.id.nPercent, "${percent.coerceAtMost(999)}%")
            setTextViewText(R.id.nTitle, "${Fmt.int(steps)} pas")
            setTextViewText(R.id.nText, motivation)
            setImageViewBitmap(R.id.nFlame, flame)
            setTextViewText(R.id.nStreak, streak.toString())
        }
        val big = RemoteViews(c.packageName, R.layout.notif_tracking_big).apply {
            setImageViewBitmap(R.id.nRing, ringBig)
            setTextViewText(R.id.nPercent, "${percent.coerceAtMost(999)}%")
            setTextViewText(R.id.nTitle, "${Fmt.int(steps)} pas")
            setTextViewText(R.id.nGoal, "sur ${Fmt.int(goal)} · $motivation")
            setImageViewBitmap(R.id.nFlame, flame)
            setTextViewText(R.id.nStreak, streak.toString())
            setProgressBar(R.id.nProgress, 1000, (ratio.coerceAtMost(1f) * 1000).toInt(), false)
            setTextViewText(R.id.nKm, "📍 ${Fmt.km(km)} km")
            setTextViewText(R.id.nKcal, "🔥 ${Fmt.int(kcal)} kcal")
            setTextViewText(R.id.nMin, "⏱ $minutes min")
        }

        return NotificationCompat.Builder(c, CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.VIOLET)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setContentTitle("${Fmt.int(steps)} / ${Fmt.int(goal)} pas")
            .setContentText(motivation)
            .setContentIntent(openApp(c))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateTrackingNotification(c: Context, steps: Int) {
        ensureChannelsOnce(c)
        try { nm(c).notify(NOTIF_TRACKING_ID, buildTrackingNotification(c, steps)) } catch (e: Exception) { }
    }

    private var channelsReady = false
    private fun ensureChannelsOnce(c: Context) {
        if (!channelsReady) { ensureChannels(c); channelsReady = true }
    }

    // --------------------------------------------------------- célébrations

    private fun celebrate(
        c: Context, id: Int, channel: String, title: String, text: String,
        emoji: String, colorA: Int, colorB: Int, cardTitle: String, cardSub: String, tab: Int = 0
    ) {
        ensureChannelsOnce(c)
        val picture = BitmapRenderer.celebration(cardTitle, cardSub, emoji, colorA, colorB)
        val notif = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(colorB)
            .setLargeIcon(BitmapRenderer.emojiBadge(emoji, colorA, colorB))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(picture).setSummaryText(text))
            .setContentIntent(openApp(c, tab))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { nm(c).notify(id, notif) } catch (e: Exception) { }
    }

    fun showGoalReached(c: Context, steps: Int) {
        val streak = StepPrefs.getStreak(c)
        celebrate(
            c, NOTIF_GOAL_ID, CHANNEL_CELEBRATION,
            "Objectif atteint ! 🎉",
            "${Fmt.int(steps)} pas · série de ${Fmt.days(streak)} 🔥",
            "🏆", Palette.VIOLET, Palette.ORANGE,
            "OBJECTIF ATTEINT", "${Fmt.int(steps)} pas · 🔥 ${Fmt.days(streak)} de série"
        )
    }

    fun showAchievement(c: Context, a: Achievement) {
        celebrate(
            c, NOTIF_ACH_BASE + Achievements.all.indexOf(a), CHANNEL_CELEBRATION,
            "Succès débloqué : ${a.title}", a.desc,
            a.emoji, Palette.PINK, Palette.VIOLET,
            a.title.uppercase(), a.desc, tab = 2
        )
    }

    fun showGradeUp(c: Context, g: Grade) {
        celebrate(
            c, NOTIF_GRADE_ID, CHANNEL_CELEBRATION,
            "Nouveau grade : ${g.name} ${g.emoji}",
            "Tu es passé·e au grade ${g.name}, continue ta série !",
            g.emoji, g.colorStart, g.colorEnd,
            "GRADE ${g.name.uppercase()}", "${Fmt.days(g.minStreak)} de série, respect !", tab = 2
        )
    }

    fun showChallengeDone(c: Context, ch: DailyChallenge.Challenge) {
        celebrate(
            c, NOTIF_CHALLENGE_ID, CHANNEL_CELEBRATION,
            "Défi du jour réussi ! ${ch.emoji}",
            "${ch.title} · +${ch.xp} XP",
            ch.emoji, Palette.MINT, Palette.VIOLET,
            "DÉFI RÉUSSI", "${ch.title} · +${ch.xp} XP"
        )
    }

    fun showFreezeEarned(c: Context) {
        val n = StepPrefs.getFreezes(c)
        celebrate(
            c, NOTIF_FREEZE_ID, CHANNEL_CELEBRATION,
            "Gel de série gagné 🧊",
            "Il protégera ta série si tu rates un jour ($n/${StepPrefs.MAX_FREEZES})",
            "🧊", Palette.ICE, Palette.VIOLET,
            "GEL DE SÉRIE GAGNÉ", "Ta série est protégée pour un jour raté", tab = 2
        )
    }

    fun showFreezeUsed(c: Context, used: Int) {
        ensureChannelsOnce(c)
        val streak = StepPrefs.getStreak(c)
        val notif = NotificationCompat.Builder(c, CHANNEL_DANGER)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.ICE)
            .setLargeIcon(BitmapRenderer.emojiBadge("🧊", Palette.ICE, Palette.VIOLET))
            .setContentTitle("Série sauvée par ${if (used > 1) "$used gels" else "un gel"} 🧊")
            .setContentText("Ta série de ${Fmt.days(streak)} continue. Ne la lâche pas aujourd'hui !")
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .build()
        try { nm(c).notify(NOTIF_FREEZE_ID, notif) } catch (e: Exception) { }
    }

    fun showMilestone(c: Context, level: Int, steps: Int) {
        if (!StepPrefs.milestonesEnabled(c)) return
        ensureChannelsOnce(c)
        val goal = StepPrefs.dailyGoal(c)
        val left = (goal - steps).coerceAtLeast(0)
        val (title, emoji) = if (level >= 75) "Plus que ${Fmt.int(left)} pas !" to "⚡" else "Mi-parcours franchi !" to "🚀"
        val density = c.resources.displayMetrics.density
        val notif = NotificationCompat.Builder(c, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.PINK)
            .setLargeIcon(BitmapRenderer.ring((96 * density).toInt(), steps.toFloat() / goal, 0x33888888, 0.12f, false))
            .setContentTitle("$emoji $title")
            .setContentText("$level % de ton objectif · ${Fmt.int(steps)} / ${Fmt.int(goal)} pas")
            .setProgress(100, level, false)
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .setTimeoutAfter(60 * 60 * 1000L)
            .build()
        try { nm(c).notify(NOTIF_PROGRESS_ID, notif) } catch (e: Exception) { }
    }

    // ------------------------------------------------------------ alertes

    fun showStreakDanger(c: Context) {
        ensureChannelsOnce(c)
        val streak = StepPrefs.getStreak(c)
        val steps = StepPrefs.getTodaySteps(c)
        val goal = StepPrefs.dailyGoal(c)
        val left = (goal - steps).coerceAtLeast(0)
        val minutes = (left / 100).coerceAtLeast(1)
        val freezes = StepPrefs.getFreezes(c)
        val extra = if (freezes > 0) "\nTu as $freezes gel${if (freezes > 1) "s" else ""} en réserve 🧊, mais ne compte pas dessus !" else ""
        val text = "Il te manque ${Fmt.int(left)} pas (≈ $minutes min de marche) pour garder ta série de ${Fmt.days(streak)}.$extra"
        val notif = NotificationCompat.Builder(c, CHANNEL_DANGER)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.ORANGE)
            .setLargeIcon(BitmapRenderer.flame((96 * c.resources.displayMetrics.density).toInt(), true))
            .setContentTitle("🔥 Ta série de ${Fmt.days(streak)} est en danger !")
            .setContentText("Encore ${Fmt.int(left)} pas avant minuit")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { nm(c).notify(NOTIF_DANGER_ID, notif) } catch (e: Exception) { }
    }

    fun showWeeklySummary(c: Context) {
        ensureChannelsOnce(c)
        val days = StepPrefs.lastDays(c, 7)
        val total = days.sumOf { it.second.toLong() }
        val avg = (total / 7).toInt()
        val best = days.maxByOrNull { it.second }
        val goal = StepPrefs.dailyGoal(c)
        val reached = days.count { it.second >= goal }
        val km = Fitness.km(c, total)
        val style = NotificationCompat.InboxStyle()
            .addLine("👣 ${Fmt.long(total)} pas au total")
            .addLine("📊 ${Fmt.int(avg)} pas par jour en moyenne")
            .addLine("🎯 Objectif atteint $reached jour${if (reached > 1) "s" else ""} sur 7")
            .addLine("📍 ${Fmt.km(km)} km parcourus")
            .addLine("🔥 Série actuelle : ${Fmt.days(StepPrefs.getStreak(c))}")
            .setSummaryText("Résumé de la semaine")
        if (best != null && best.second > 0) style.addLine("⭐ Meilleur jour : ${Fmt.int(best.second)} pas")
        val notif = NotificationCompat.Builder(c, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.VIOLET)
            .setLargeIcon(BitmapRenderer.emojiBadge("📊", Palette.VIOLET, Palette.PINK))
            .setContentTitle("Ta semaine : ${Fmt.long(total)} pas 📊")
            .setContentText("$reached/7 objectifs · ${Fmt.km(km)} km")
            .setStyle(style)
            .setContentIntent(openApp(c, 1))
            .setAutoCancel(true)
            .build()
        try { nm(c).notify(NOTIF_SUMMARY_ID, notif) } catch (e: Exception) { }
    }

    fun showWalkReminder(c: Context) {
        if (!StepPrefs.remindersEnabled(c)) return
        ensureChannelsOnce(c)
        val goal = StepPrefs.dailyGoal(c)
        val steps = StepPrefs.getTodaySteps(c)
        val messages = listOf(
            "Une petite pause marche te ferait du bien 🚶",
            "Envie de bouger un peu ? Quelques pas t'attendent.",
            "Ton objectif du jour a besoin de toi, en route !",
            "Debout ! Même 5 minutes de marche comptent.",
            "Un tour du pâté de maisons ? Ton cœur dira merci ❤️",
            "Ta flamme a besoin d'un peu de carburant 🔥"
        )
        val notif = NotificationCompat.Builder(c, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setColor(Palette.PINK)
            .setLargeIcon(BitmapRenderer.emojiBadge("👟", Palette.PINK, Palette.VIOLET))
            .setContentTitle("Envie de marcher ? 👟")
            .setContentText(messages.random())
            .setSubText("${Fmt.int(steps)} / ${Fmt.int(goal)} pas")
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .setTimeoutAfter(2 * 60 * 60 * 1000L)
            .build()
        try { nm(c).notify(NOTIF_REMINDER_ID, notif) } catch (e: Exception) { }
    }
}
