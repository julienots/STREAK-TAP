package com.stepstreak.app

import android.Manifest
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAB = "tab"
    }

    // Pages / navigation
    private lateinit var pages: List<ScrollView>
    private lateinit var tabs: List<LinearLayout>
    private var currentTab = 0
    private lateinit var confetti: ConfettiView

    // Aujourd'hui
    private lateinit var ring: ProgressRingView
    private lateinit var tvSteps: TextView
    private var displayedSteps = 0
    private var stepsAnimator: ValueAnimator? = null
    private var firstRefresh = true

    // Stats
    private var statsRange = 7

    private val stepsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshToday()
            if (intent?.getBooleanExtra(StepService.EXTRA_GOAL_REACHED, false) == true) celebrateGoal()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startTracking() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_main)

        confetti = findViewById(R.id.confetti)
        ring = findViewById(R.id.ringView)
        tvSteps = findViewById(R.id.tvSteps)
        pages = listOf(findViewById(R.id.pageToday), findViewById(R.id.pageStats), findViewById(R.id.pageTrophies))
        tabs = listOf(findViewById(R.id.tabToday), findViewById(R.id.tabStats), findViewById(R.id.tabTrophies))

        applyInsets()

        tabs.forEachIndexed { i, tab ->
            tab.setOnClickListener { it.haptic(); selectTab(i) }
        }
        findViewById<View>(R.id.btnSettings).apply { pressable(); setOnClickListener { haptic(); showSettings() } }
        findViewById<View>(R.id.tvGoalChip).apply { pressable(); setOnClickListener { haptic(); showSettings() } }
        findViewById<View>(R.id.cardStreak).apply { pressable(); setOnClickListener { haptic(); selectTab(2) } }
        findViewById<View>(R.id.tvLevelChip).apply { pressable(); setOnClickListener { haptic(); selectTab(2) } }
        findViewById<View>(R.id.btnBattery).apply { pressable(); setOnClickListener { requestIgnoreBatteryOptimizations() } }
        findViewById<View>(R.id.ringContainer).setOnClickListener {
            it.haptic()
            it.animate().scaleX(0.94f).scaleY(0.94f).setDuration(110).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(OvershootInterpolator(4f)).start()
            }.start()
            if (StepPrefs.getTodaySteps(this) >= StepPrefs.dailyGoal(this)) confetti.burst(90)
        }
        findViewById<TextView>(R.id.seg7).setOnClickListener { it.haptic(); statsRange = 7; refreshStats() }
        findViewById<TextView>(R.id.seg30).setOnClickListener { it.haptic(); statsRange = 30; refreshStats() }
        findViewById<TextView>(R.id.seg90).setOnClickListener { it.haptic(); statsRange = 90; refreshStats() }

        buildHeatLegend()
        selectTab(intent?.getIntExtra(EXTRA_TAB, 0) ?: 0, animate = false)
        playEntrance(findViewById(R.id.todayContent))
        requestNeededPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(EXTRA_TAB)) selectTab(intent.getIntExtra(EXTRA_TAB, 0))
    }

    override fun onResume() {
        super.onResume()
        StepPrefs.checkMissedStreak(this)
        val filter = IntentFilter(StepService.ACTION_STEPS_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stepsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stepsReceiver, filter)
        }
        refreshAll()
        WidgetUpdater.updateAll(this)
        if (StepPrefs.getTodaySteps(this) >= StepPrefs.dailyGoal(this) && StepPrefs.shouldShowConfettiToday(this)) {
            confetti.postDelayed({ celebrateGoal() }, 900)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(stepsReceiver) } catch (e: Exception) { }
    }

    // ------------------------------------------------------------ insets

    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        val nav = findViewById<View>(R.id.bottomNav)
        val baseNavMargin = dpi(18f)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            pages.forEach { p ->
                val content = p.getChildAt(0)
                content.setPadding(content.paddingLeft, dpi(20f) + bars.top, content.paddingRight, dpi(120f) + bars.bottom)
            }
            (nav.layoutParams as FrameLayout.LayoutParams).bottomMargin = baseNavMargin + bars.bottom
            nav.requestLayout()
            insets
        }
    }

    // ------------------------------------------------------------ onglets

    private fun selectTab(index: Int, animate: Boolean = true) {
        val target = index.coerceIn(0, 2)
        val previous = currentTab
        currentTab = target
        tabs.forEachIndexed { i, tab ->
            val selected = i == target
            tab.setBackgroundResource(if (selected) R.drawable.bg_tab_selected else android.R.color.transparent)
            tab.alpha = if (selected) 1f else 0.55f
            (tab.getChildAt(1) as TextView).visibility = if (selected) View.VISIBLE else View.GONE
            if (selected && animate) {
                tab.scaleX = 0.85f; tab.scaleY = 0.85f
                tab.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(OvershootInterpolator(3f)).start()
            }
        }
        pages.forEachIndexed { i, page ->
            if (i == target) {
                page.visibility = View.VISIBLE
                if (animate && previous != target) {
                    page.alpha = 0f
                    page.translationX = dp(if (target > previous) 40f else -40f)
                    page.animate().alpha(1f).translationX(0f).setDuration(320)
                        .setInterpolator(DecelerateInterpolator(2f)).start()
                    playEntrance(page.getChildAt(0) as ViewGroup)
                }
            } else page.visibility = View.GONE
        }
        when (target) {
            1 -> refreshStats()
            2 -> refreshTrophies()
        }
    }

    private fun playEntrance(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            v.alpha = 0f
            v.translationY = dp(28f)
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(60L + i * 55L).setDuration(520)
                .setInterpolator(DecelerateInterpolator(2.2f)).start()
        }
    }

    // ------------------------------------------------------- rafraîchissements

    private fun refreshAll() {
        refreshToday()
        when (currentTab) {
            1 -> refreshStats()
            2 -> refreshTrophies()
        }
    }

    private fun refreshToday() {
        val steps = StepPrefs.getTodaySteps(this)
        val goal = StepPrefs.dailyGoal(this)
        val streak = StepPrefs.getStreak(this)
        val longest = StepPrefs.getLongestStreak(this)
        val ratio = if (goal > 0) steps.toFloat() / goal else 0f
        val grade = Grades.forStreak(streak)
        val level = Levels.info(this)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        text(R.id.tvGreeting, "${Motivation.greeting(hour)} 👋")
        text(R.id.tvDate, SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() })
        text(R.id.tvLevelChip, "⭐ Niv. ${level.level}")

        animateSteps(steps)
        ring.setProgress(ratio, animate = true)
        text(R.id.tvGoalLine, "sur ${Fmt.int(goal)} pas")
        text(R.id.tvPercent, if (ratio >= 1f) "✓ ${(ratio * 100).toInt()} %" else "${(ratio * 100).toInt()} %")
        findViewById<TextView>(R.id.tvPercent).setBackgroundResource(
            if (ratio >= 1f) R.drawable.bg_pill_success else R.drawable.bg_pill_gradient
        )
        text(R.id.tvRingEmoji, when {
            ratio >= 1f -> "🏆"
            ratio >= 0.5f -> "🏃"
            else -> "👟"
        })
        text(R.id.tvMotivation, Motivation.forProgress(steps, goal, streak))
        text(R.id.tvGoalChip, "🎯  Objectif ${Fmt.int(goal)}  ·  modifier")

        text(R.id.tvKm, Fmt.km(Fitness.km(this, steps.toLong())))
        text(R.id.tvKcal, Fmt.int(Fitness.kcal(this, steps.toLong())))
        text(R.id.tvMin, Fitness.activeMinutes(steps.toLong()).toString())

        // Série
        findViewById<FlameView>(R.id.flameView).active = streak > 0
        text(R.id.tvStreakCount, streak.toString())
        text(R.id.tvStreakUnit, if (streak > 1) " jours" else " jour")
        text(R.id.tvStreakRecord, when {
            StepPrefs.streakAtRisk(this) -> "⚠️ Valide aujourd'hui pour la garder"
            streak == 0 -> "Atteins ton objectif pour l'allumer"
            streak >= longest -> "🏅 C'est ton record !"
            else -> "Record : ${Fmt.days(longest)}"
        })
        text(R.id.tvGradeChip, "${grade.emoji} ${grade.name}")
        text(R.id.tvFreezeChip, "🧊 ${StepPrefs.getFreezes(this)}/${StepPrefs.MAX_FREEZES}")

        // Défi du jour
        val ch = DailyChallenge.today(this)
        val done = StepPrefs.challengeDoneToday(this)
        text(R.id.tvChallengeEmoji, ch.emoji)
        text(R.id.tvChallengeTitle, ch.title)
        text(R.id.tvChallengeDesc, if (done) "Réussi ! +${ch.xp} XP gagnés 🎉" else "${ch.desc} · ${Fmt.int(ch.target)} pas")
        text(R.id.tvChallengeXp, if (done) "✓" else "+${ch.xp} XP")
        val chBar = findViewById<GradientBarView>(R.id.challengeBar)
        if (done) chBar.setColors(Palette.MINT, Palette.ICE) else chBar.setColors(Palette.VIOLET, Palette.PINK)
        chBar.setProgress(if (ch.target > 0) steps.toFloat() / ch.target else 0f, 300)

        // Semaine
        val history = StepPrefs.history(this)
        val week = StepPrefs.lastDays(this, 7, history)
        val frozen = StepPrefs.getFrozenDays(this)
        val today = StepPrefs.todayKey()
        findViewById<WeekStripView>(R.id.weekStrip).setDays(week.map { (k, v) ->
            WeekStripView.Day(WidgetUpdater.dayLetter(k), if (goal > 0) v.toFloat() / goal else 0f, k == today, k in frozen, false)
        })
        text(R.id.tvWeekTotal, "${Fmt.long(week.sumOf { it.second.toLong() })} pas")

        // Batterie
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        findViewById<View>(R.id.cardBattery).visibility =
            if (pm.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE

        firstRefresh = false
    }

    private fun refreshStats() {
        listOf(R.id.seg7 to 7, R.id.seg30 to 30, R.id.seg90 to 90).forEach { (id, n) ->
            val tv = findViewById<TextView>(id)
            val sel = n == statsRange
            tv.setBackgroundResource(if (sel) R.drawable.bg_tab_selected else android.R.color.transparent)
            tv.setTextColor(if (sel) Color.WHITE else 0x99FFFFFF.toInt())
        }
        val goal = StepPrefs.dailyGoal(this)
        val history = StepPrefs.history(this)
        val days = StepPrefs.lastDays(this, statsRange, history)
        val chart = findViewById<BarChartView>(R.id.chart)
        val bars = if (statsRange <= 30) {
            days.map { (k, v) ->
                BarChartView.Bar(if (statsRange == 7) WidgetUpdater.dayLetter(k) else dayOfMonth(k), v, k)
            }
        } else {
            // 90 jours : regroupé par semaine (moyenne quotidienne)
            days.chunked(7).map { chunk ->
                BarChartView.Bar(dayOfMonth(chunk.first().first), chunk.map { it.second }.average().toInt(), chunk.first().first)
            }
        }
        chart.onSelect = { i ->
            if (i in bars.indices) {
                val b = bars[i]
                val label = if (statsRange > 30) "Semaine du ${prettyDate(b.key)} (moy.)" else prettyDate(b.key)
                text(R.id.tvChartSelected, "${Fmt.int(b.value)} pas")
                text(R.id.tvChartSelectedDate, label)
            }
        }
        chart.setData(bars, goal)

        val values = days.map { it.second }
        val total = values.sumOf { it.toLong() }
        val activeDays = values.count { it > 0 }.coerceAtLeast(1)
        text(R.id.tvSumTotal, Fmt.compact(total))
        text(R.id.tvSumAvg, Fmt.compact(total / activeDays))
        text(R.id.tvSumReached, "${values.count { it >= goal }}/${values.size}")

        // Heatmap 18 semaines, colonnes lundi -> dimanche
        val weeks = 18
        val cal = Calendar.getInstance()
        val todayKey = StepPrefs.todayKey()
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // lundi = 0
        cal.add(Calendar.DAY_OF_YEAR, -dow - (weeks - 1) * 7)
        val frozen = StepPrefs.getFrozenDays(this)
        var reachedToday = false
        val cols = ArrayList<List<HeatmapView.Cell>>()
        repeat(weeks) {
            val col = ArrayList<HeatmapView.Cell>()
            repeat(7) {
                val k = StepPrefs.dayKey(cal.time)
                val future = k > todayKey
                val v = history[k] ?: 0
                col.add(HeatmapView.Cell(if (goal > 0) v.toFloat() / goal else 0f, k in frozen, k == todayKey, future))
                if (k == todayKey) reachedToday = v >= goal
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cols.add(col)
        }
        findViewById<HeatmapView>(R.id.heatmap).setData(cols)
        val reachedCount = history.count { it.value >= goal && it.key <= todayKey }
        text(R.id.tvHeatSub, "$reachedCount jours objectif atteint" + if (reachedToday) " · dont aujourd'hui 🔥" else "")

        // Records
        val totalAll = StepPrefs.getTotalSteps(this)
        val best = StepPrefs.getBestDay(this)
        text(R.id.tvRecTotal, Fmt.compact(totalAll))
        text(R.id.tvRecKm, Fmt.km(Fitness.km(this, totalAll)))
        text(R.id.tvRecBest, Fmt.int(best.first))
        text(R.id.tvRecBestDate, best.second?.let { prettyDate(it) } ?: "—")
        text(R.id.tvRecStreak, StepPrefs.getLongestStreak(this).toString())
        text(R.id.tvRecKcal, Fmt.compact(Fitness.kcal(this, totalAll).toLong()))
        text(R.id.tvRecGoalDays, StepPrefs.getGoalDays(this).toString())
    }

    private fun refreshTrophies() {
        val streak = StepPrefs.getStreak(this)
        val grade = Grades.forStreak(streak)
        val next = Grades.next(streak)

        // Carte grade
        val card = findViewById<View>(R.id.cardGrade)
        card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(grade.colorStart.withAlpha(0x66), grade.colorEnd.withAlpha(0x33), 0x14FFFFFF)
        ).apply {
            cornerRadius = dp(28f)
            setStroke(dpi(1f), grade.colorStart.withAlpha(0x88))
        }
        val emojiCircle = findViewById<TextView>(R.id.tvGradeEmojiBig)
        emojiCircle.text = grade.emoji
        emojiCircle.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(grade.colorStart, grade.colorEnd)
        ).apply { shape = GradientDrawable.OVAL }
        emojiCircle.rotation = -25f
        emojiCircle.animate().rotation(0f).setDuration(900).setInterpolator(OvershootInterpolator(3f)).start()
        text(R.id.tvGradeName, grade.name)
        val bar = findViewById<GradientBarView>(R.id.gradeBar)
        bar.setColors(grade.colorStart, grade.colorEnd)
        if (next != null) {
            text(R.id.tvGradeNext, "Encore ${Fmt.days(next.minStreak - streak)} pour ${next.emoji} ${next.name}")
            bar.setProgress((streak - grade.minStreak).toFloat() / (next.minStreak - grade.minStreak), 250)
        } else {
            text(R.id.tvGradeNext, "Grade suprême atteint. Respect absolu 🙇")
            bar.setProgress(1f, 250)
        }
        val ladder = findViewById<LinearLayout>(R.id.gradeLadder)
        ladder.removeAllViews()
        Grades.all.forEach { g ->
            val reached = streak >= g.minStreak
            ladder.addView(TextView(this).apply {
                text = g.emoji
                textSize = if (g == grade) 22f else 16f
                gravity = Gravity.CENTER
                alpha = if (reached) 1f else 0.28f
                layoutParams = LinearLayout.LayoutParams(0, dpi(40f), 1f)
            })
        }

        // Niveau
        val lv = Levels.info(this)
        text(R.id.tvLevel, "Niveau ${lv.level}")
        text(R.id.tvLevelTitle, Levels.title(lv.level))
        text(R.id.tvXp, "${Fmt.long(lv.xpInLevel)} / ${Fmt.long(lv.xpForNext)} XP")
        findViewById<GradientBarView>(R.id.xpBar).apply {
            setColors(Palette.YELLOW, Palette.ORANGE)
            setProgress(lv.xpInLevel.toFloat() / lv.xpForNext, 400)
        }

        // Gels
        val freezes = StepPrefs.getFreezes(this)
        text(R.id.tvFreezeTitle, "Gels de série · $freezes/${StepPrefs.MAX_FREEZES}")
        text(R.id.tvFreezeIcons, "🧊".repeat(freezes) + "▫️".repeat(StepPrefs.MAX_FREEZES - freezes))

        // Succès
        val snap = Achievements.snapshot(this)
        val unlocked = Achievements.all.count { Achievements.isUnlocked(it, snap) }
        text(R.id.tvAchCount, "$unlocked / ${Achievements.all.size}")
        val container = findViewById<LinearLayout>(R.id.achievementsContainer)
        container.removeAllViews()
        var tileIndex = 0
        AchCategory.values().forEach { cat ->
            val list = Achievements.all.filter { it.category == cat }
            if (list.isEmpty()) return@forEach
            container.addView(TextView(this).apply {
                text = "${cat.emoji}  ${cat.label}"
                setTextColor(0xCCFFFFFF.toInt())
                textSize = 14f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                setPadding(dpi(4f), dpi(14f), 0, dpi(8f))
            })
            list.chunked(3).forEach { rowItems ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                rowItems.forEach { a ->
                    val tile = buildAchievementTile(a, snap)
                    tile.alpha = 0f
                    tile.scaleX = 0.8f; tile.scaleY = 0.8f
                    tile.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(150L + tileIndex * 35L).setDuration(380)
                        .setInterpolator(OvershootInterpolator(1.6f)).start()
                    tileIndex++
                    row.addView(tile)
                }
                repeat(3 - rowItems.size) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                }
                container.addView(row)
            }
        }
    }

    private fun buildAchievementTile(a: Achievement, snap: Achievements.Snapshot): View {
        val unlocked = Achievements.isUnlocked(a, snap)
        val progress = Achievements.progress(a, snap)
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(if (unlocked) R.drawable.bg_tile_unlocked else R.drawable.bg_tile_locked)
            setPadding(dpi(8f), dpi(14f), dpi(8f), dpi(12f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dpi(4f), dpi(4f), dpi(4f), dpi(4f))
            }
        }
        tile.addView(TextView(this).apply {
            text = if (unlocked) a.emoji else "🔒"
            textSize = 28f
            gravity = Gravity.CENTER
            alpha = if (unlocked) 1f else 0.6f
        })
        tile.addView(TextView(this).apply {
            text = a.title
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 2
            setTextColor(if (unlocked) Color.WHITE else 0x88FFFFFF.toInt())
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            setPadding(0, dpi(6f), 0, dpi(6f))
        })
        if (unlocked) {
            tile.addView(TextView(this).apply {
                text = "DÉBLOQUÉ"
                textSize = 9f
                letterSpacing = 0.12f
                gravity = Gravity.CENTER
                setTextColor(Palette.YELLOW)
            })
        } else {
            tile.addView(GradientBarView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpi(5f)).apply {
                    setMargins(dpi(6f), dpi(2f), dpi(6f), 0)
                }
                setProgress(progress, 300)
            })
        }
        tile.pressable()
        tile.setOnClickListener {
            it.haptic()
            val v = Achievements.value(a, snap).coerceAtMost(a.target)
            val msg = if (unlocked) "${a.desc}\n\n✅ Débloqué !"
            else "${a.desc}\n\nProgression : ${Achievements.formatValue(a, v)} / ${Achievements.formatValue(a, a.target)} (${(progress * 100).toInt()} %)"
            AlertDialog.Builder(this, R.style.Dialog_StepStreak)
                .setTitle("${if (unlocked) a.emoji else "🔒"}  ${a.title}")
                .setMessage(msg)
                .setPositiveButton("Super", null)
                .show()
        }
        return tile
    }

    // -------------------------------------------------------------- célébration

    private fun celebrateGoal() {
        StepPrefs.markConfettiShown(this)
        confetti.burst()
        SoundPlayer.playGoalReached(this)
        ring.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180).withEndAction {
            ring.animate().scaleX(1f).scaleY(1f).setDuration(600).setInterpolator(OvershootInterpolator(5f)).start()
        }.start()
    }

    private fun animateSteps(target: Int) {
        stepsAnimator?.cancel()
        val start = if (firstRefresh) 0 else displayedSteps
        if (start == target) { tvSteps.text = Fmt.int(target); return }
        stepsAnimator = ValueAnimator.ofInt(start, target).apply {
            duration = if (firstRefresh) 1400 else 600
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { tvSteps.text = Fmt.int(it.animatedValue as Int) }
            start()
        }
        displayedSteps = target
    }

    // ------------------------------------------------------------- réglages

    private fun showSettings() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheet_StepStreak)
        val v = layoutInflater.inflate(R.layout.sheet_settings, null)
        dialog.setContentView(v)

        val tvGoal = v.findViewById<TextView>(R.id.tvGoalValue)
        val tvGoalHint = v.findViewById<TextView>(R.id.tvGoalHint)
        val slider = v.findViewById<Slider>(R.id.sliderGoal)
        fun showGoal(goal: Int) {
            tvGoal.text = Fmt.int(goal)
            val km = Fitness.km(this, goal.toLong())
            tvGoalHint.text = "≈ ${Fmt.km(km)} km · ${Fitness.activeMinutes(goal.toLong())} min de marche · ${Fmt.int(Fitness.kcal(this, goal.toLong()))} kcal"
        }
        val current = ((StepPrefs.dailyGoal(this) / 500) * 500).coerceIn(1000, 30000)
        slider.value = current.toFloat()
        showGoal(current)
        slider.addOnChangeListener { s, value, fromUser ->
            showGoal(value.toInt())
            if (fromUser && value.toInt() % 2000 == 0) s.haptic()
        }
        listOf(R.id.preset5k to 5000, R.id.preset8k to 8000, R.id.preset10k to 10000, R.id.preset12k to 12000, R.id.preset15k to 15000)
            .forEach { (id, value) ->
                v.findViewById<View>(id).apply {
                    pressable()
                    setOnClickListener { haptic(); slider.value = value.toFloat() }
                }
            }

        val tvHeight = v.findViewById<TextView>(R.id.tvHeight)
        val tvWeight = v.findViewById<TextView>(R.id.tvWeight)
        var height = StepPrefs.heightCm(this)
        var weight = StepPrefs.weightKg(this)
        fun showProfile() {
            tvHeight.text = "$height cm"
            tvWeight.text = "$weight kg"
            showGoal(slider.value.toInt())
        }
        showProfile()
        v.findViewById<View>(R.id.btnHeightMinus).setOnClickListener { it.haptic(); height = (height - 1).coerceAtLeast(100); StepPrefs.setHeightCm(this, height); showProfile() }
        v.findViewById<View>(R.id.btnHeightPlus).setOnClickListener { it.haptic(); height = (height + 1).coerceAtMost(230); StepPrefs.setHeightCm(this, height); showProfile() }
        v.findViewById<View>(R.id.btnWeightMinus).setOnClickListener { it.haptic(); weight = (weight - 1).coerceAtLeast(30); StepPrefs.setWeightKg(this, weight); showProfile() }
        v.findViewById<View>(R.id.btnWeightPlus).setOnClickListener { it.haptic(); weight = (weight + 1).coerceAtMost(250); StepPrefs.setWeightKg(this, weight); showProfile() }

        fun bindSwitch(id: Int, value: Boolean, onChange: (Boolean) -> Unit) {
            v.findViewById<SwitchMaterial>(id).apply {
                isChecked = value
                setOnCheckedChangeListener { b, checked -> b.haptic(); onChange(checked) }
            }
        }
        bindSwitch(R.id.switchSounds, StepPrefs.soundsEnabled(this)) { StepPrefs.setSoundsEnabled(this, it) }
        bindSwitch(R.id.switchMilestones, StepPrefs.milestonesEnabled(this)) { StepPrefs.setMilestonesEnabled(this, it) }
        bindSwitch(R.id.switchReminders, StepPrefs.remindersEnabled(this)) {
            StepPrefs.setRemindersEnabled(this, it)
            if (it) AlarmScheduler.scheduleReminder(this) else AlarmScheduler.cancelReminder(this)
        }
        bindSwitch(R.id.switchEvening, StepPrefs.eveningAlertEnabled(this)) { StepPrefs.setEveningAlertEnabled(this, it) }
        bindSwitch(R.id.switchHaptics, StepPrefs.hapticsEnabled(this)) { StepPrefs.setHapticsEnabled(this, it) }

        v.findViewById<View>(R.id.btnBatterySheet).setOnClickListener { requestIgnoreBatteryOptimizations() }
        v.findViewById<View>(R.id.btnNotifSettings).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (e: Exception) { }
        }
        v.findViewById<View>(R.id.btnTestCelebration).setOnClickListener {
            dialog.dismiss()
            confetti.postDelayed({ celebrateGoal() }, 250)
        }

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundColor(Color.TRANSPARENT)
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        dialog.setOnDismissListener {
            val newGoal = slider.value.toInt()
            if (newGoal != StepPrefs.dailyGoal(this)) {
                StepPrefs.setDailyGoal(this, newGoal)
                StepPrefs.prefs(this).edit().remove("challenge_date").apply()
            }
            refreshAll()
            WidgetUpdater.updateAll(this)
            NotificationHelper.updateTrackingNotification(this, StepPrefs.getTodaySteps(this))
        }
        dialog.show()
    }

    // ------------------------------------------------------------ permissions

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) needed.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val toRequest = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) permissionLauncher.launch(toRequest.toTypedArray()) else startTracking()
    }

    private fun startTracking() {
        StepService.start(this)
        AlarmScheduler.scheduleAll(this)
        refreshToday()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName")))
            } catch (e: Exception) { }
        } else {
            Toast.makeText(this, "Déjà autorisé ✅", Toast.LENGTH_SHORT).show()
        }
    }

    // --------------------------------------------------------------- helpers

    private fun text(id: Int, value: CharSequence) { findViewById<TextView>(id).text = value }

    private fun prettyDate(key: String): String {
        val d = StepPrefs.parseDay(key) ?: return key
        return when (key) {
            StepPrefs.todayKey() -> "Aujourd'hui"
            StepPrefs.dayKeyOffset(-1) -> "Hier"
            else -> SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(d).replaceFirstChar { it.uppercase() }
        }
    }

    private fun dayOfMonth(key: String): String {
        val d = StepPrefs.parseDay(key) ?: return ""
        return SimpleDateFormat("d", Locale.FRENCH).format(d)
    }

    private fun buildHeatLegend() {
        val legend = findViewById<LinearLayout>(R.id.heatLegend)
        fun square(color: Int) = View(this).apply {
            background = GradientDrawable().apply { setColor(color); cornerRadius = dp(3f) }
            layoutParams = LinearLayout.LayoutParams(dpi(12f), dpi(12f)).apply { marginEnd = dpi(4f) }
        }
        fun label(s: String) = TextView(this).apply {
            text = s; textSize = 11f; setTextColor(0x88FFFFFF.toInt())
            setPadding(dpi(2f), 0, dpi(10f), 0)
        }
        legend.addView(label("Moins"))
        HeatmapView.LEVELS.forEach { legend.addView(square(it)) }
        legend.addView(label("Objectif"))
        legend.addView(square(HeatmapView.FROZEN))
        legend.addView(label("Gel"))
    }
}
