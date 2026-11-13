package com.stepstreak.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private fun View.seconds(): Float = (SystemClock.uptimeMillis() % 1_000_000L) / 1000f

// =========================================================== Anneau principal

class ProgressRingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val painter = RingPainter()
    private val stroke = context.dp(20f)
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = context.dp(2f); strokeCap = Paint.Cap.ROUND; color = 0x2EFFFFFF
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dotGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.YELLOW }

    fun setProgress(fraction: Float, animate: Boolean = true) {
        val target = fraction.coerceIn(0f, 2f)
        animator?.cancel()
        if (!animate) { progress = target; invalidate(); return }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 1400
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - stroke * 1.6f
        val t = seconds()

        // Graduations façon cadran
        val outer = radius + stroke * 1.25f
        val inner = radius + stroke * 0.95f
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val reached = i / 60f < progress
            tickPaint.alpha = if (i % 5 == 0) 120 else 46
            if (reached) tickPaint.alpha = 200
            val o = if (i % 5 == 0) outer + stroke * 0.12f else outer
            canvas.drawLine(
                cx + (inner * cos(a)).toFloat(), cy + (inner * sin(a)).toFloat(),
                cx + (o * cos(a)).toFloat(), cy + (o * sin(a)).toFloat(), tickPaint
            )
        }

        val tip = painter.draw(canvas, cx, cy, radius, stroke, progress, 0x1FFFFFFF, true)

        if (progress > 0.005f) {
            val a = Math.toRadians(tip.toDouble())
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            val pulse = 0.5f + 0.5f * sin(t * 4f)
            dotGlow.alpha = (50 + 70 * pulse).toInt()
            canvas.drawCircle(x, y, stroke * (0.75f + 0.35f * pulse), dotGlow)
            canvas.drawCircle(x, y, stroke * 0.3f, dotPaint)
        }

        // Étincelles qui tournent autour quand l'objectif est atteint
        if (progress >= 1f) {
            for (i in 0 until 10) {
                val ang = t * (0.6f + i * 0.07f) + i * 0.628f
                val rr = radius + stroke * (1.9f + 0.5f * sin(t * 2f + i))
                val sx = cx + rr * cos(ang)
                val sy = cy + rr * sin(ang)
                sparkPaint.color = if (i % 2 == 0) Palette.YELLOW else Palette.PINK
                sparkPaint.alpha = (120 + 120 * sin(t * 3f + i)).toInt().coerceIn(0, 255)
                canvas.drawCircle(sx, sy, context.dp(2.2f), sparkPaint)
            }
        }
        if (isShown) postInvalidateOnAnimation()
    }
}

// ================================================================== Flamme

class FlameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val painter = FlamePainter()
    private val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var active = true
        set(v) { field = v; invalidate() }

    private data class Ember(var x: Float, var y: Float, var vy: Float, var life: Float, var size: Float)
    private val embers = ArrayList<Ember>()
    private var last = 0L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = seconds()
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else min((now - last) / 1000f, 0.05f)
        last = now

        if (active) {
            if (embers.size < 14 && Random.nextFloat() < 0.25f) {
                embers.add(
                    Ember(
                        width / 2f + (Random.nextFloat() - 0.5f) * width * 0.4f,
                        height * 0.75f, context.dp(30f + Random.nextFloat() * 40f),
                        1f, context.dp(1.2f + Random.nextFloat() * 1.8f)
                    )
                )
            }
            val it = embers.iterator()
            while (it.hasNext()) {
                val e = it.next()
                e.y -= e.vy * dt
                e.x += sin(t * 6f + e.size) * context.dp(0.4f)
                e.life -= dt * 0.9f
                if (e.life <= 0f || e.y < 0f) { it.remove(); continue }
                emberPaint.color = if (e.size > context.dp(2f)) Palette.YELLOW else Palette.ORANGE
                emberPaint.alpha = (255 * e.life).toInt().coerceIn(0, 255)
                canvas.drawCircle(e.x, e.y, e.size, emberPaint)
            }
        } else embers.clear()

        val pad = width * 0.08f
        painter.draw(canvas, pad, pad, width - 2 * pad, height - 2 * pad, t, active)
        if (isShown) postInvalidateOnAnimation()
    }
}

// ============================================================ Fond aurora

class AuroraView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val blobColors = intArrayOf(0x667C5CFF, 0x4DFF4FA3, 0x3D3DB8FF, 0x33FF8A3D)
    private val paints = Array(4) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var radius = 0f
    var tint: Int = 0
        set(v) { field = v; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = max(w, h) * 0.55f
        for (i in 0 until 4) {
            paints[i].shader = RadialGradient(0f, 0f, radius, blobColors[i], Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Palette.BG)
        val t = seconds() * 0.12f
        val w = width.toFloat()
        val h = height.toFloat()
        val pos = arrayOf(
            floatArrayOf(w * (0.15f + 0.15f * sin(t)), h * (0.08f + 0.06f * cos(t * 1.3f))),
            floatArrayOf(w * (0.95f + 0.1f * cos(t * 0.8f)), h * (0.30f + 0.10f * sin(t * 0.9f))),
            floatArrayOf(w * (0.10f + 0.2f * sin(t * 0.7f + 2f)), h * (0.80f + 0.08f * cos(t))),
            floatArrayOf(w * (0.80f + 0.12f * sin(t * 1.1f + 1f)), h * (1.0f + 0.05f * sin(t * 1.4f)))
        )
        for (i in 0 until 4) {
            canvas.save()
            canvas.translate(pos[i][0], pos[i][1])
            canvas.drawCircle(0f, 0f, radius, paints[i])
            canvas.restore()
        }
        if (tint != 0) canvas.drawColor(tint)
        if (isShown) postInvalidateOnAnimation()
    }
}

// =============================================================== Confettis

class ConfettiView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private class P(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var rot: Float, val vr: Float, var flip: Float, val vf: Float,
        val w: Float, val h: Float, val color: Int, val circle: Boolean, var life: Float
    )

    private val ps = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L
    private val colors = intArrayOf(
        Palette.VIOLET, Palette.PINK, Palette.ORANGE, Palette.YELLOW, Palette.MINT, Palette.ICE, Color.WHITE
    )

    fun burst(count: Int = 180) {
        if (width == 0) return
        val d = resources.displayMetrics.density
        repeat(count) { i ->
            val left = i % 2 == 0
            val angle = Math.toRadians((if (left) -60.0 else -120.0) + (Random.nextDouble() - 0.5) * 50)
            val speed = (900 + Random.nextFloat() * 1300) * d / 2.2f
            ps.add(
                P(
                    x = if (left) -10f else width + 10f,
                    y = height * 0.72f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    rot = Random.nextFloat() * 360f,
                    vr = (Random.nextFloat() - 0.5f) * 720f,
                    flip = Random.nextFloat() * 6f,
                    vf = 4f + Random.nextFloat() * 8f,
                    w = (5 + Random.nextFloat() * 6) * d,
                    h = (8 + Random.nextFloat() * 8) * d,
                    color = colors[Random.nextInt(colors.size)],
                    circle = Random.nextFloat() < 0.25f,
                    life = 1f
                )
            )
        }
        last = 0L
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        if (ps.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else min((now - last) / 1000f, 0.04f)
        last = now
        val g = 900f * resources.displayMetrics.density / 2.2f
        val it = ps.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vy += g * dt
            p.vx *= (1f - 0.9f * dt)
            p.vy *= (1f - 0.4f * dt)
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vr * dt
            p.flip += p.vf * dt
            if (p.vy > 0) p.life -= dt * 0.35f
            if (p.y > height + 40 || p.life <= 0f) { it.remove(); continue }
            paint.color = p.color
            paint.alpha = (255 * p.life.coerceIn(0f, 1f)).toInt()
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rot)
            if (p.circle) {
                canvas.drawCircle(0f, 0f, p.w * 0.5f, paint)
            } else {
                val sx = abs(cos(p.flip))
                canvas.drawRect(-p.w / 2 * sx, -p.h / 2, p.w / 2 * sx, p.h / 2, paint)
            }
            canvas.restore()
        }
        if (ps.isNotEmpty()) postInvalidateOnAnimation()
    }
}

// ======================================================= Barre de progression

class GradientBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var progress = 0f
    private var colorA = Palette.VIOLET
    private var colorB = Palette.PINK
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var animator: ValueAnimator? = null

    fun setColors(a: Int, b: Int) { colorA = a; colorB = b; invalidate() }

    fun setProgress(f: Float, delay: Long = 0) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, f.coerceIn(0f, 1f)).apply {
            duration = 1100; startDelay = delay
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val r = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, r, r, track)
        val fw = max(height.toFloat(), width * progress)
        if (progress <= 0f) return
        rect.set(0f, 0f, fw, height.toFloat())
        fill.shader = LinearGradient(0f, 0f, fw, 0f, colorA, colorB, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, fill)
        // reflet qui glisse
        val t = (seconds() % 2.6f) / 2.6f
        val sx = -fw * 0.3f + t * fw * 1.6f
        shine.shader = LinearGradient(sx - fw * 0.15f, 0f, sx + fw * 0.15f, 0f,
            intArrayOf(Color.TRANSPARENT, 0x55FFFFFF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, shine)
        if (isShown) postInvalidateOnAnimation()
    }
}

// ================================================================ Semaine

class WeekStripView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    data class Day(val letter: String, val ratio: Float, val isToday: Boolean, val frozen: Boolean, val future: Boolean)

    private var days: List<Day> = emptyList()
    private var anim = 0f
    private val painter = RingPainter()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = context.dp(12f); color = 0x99FFFFFF.toInt()
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = context.dp(13f) }
    private val todayBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
    private val rect = RectF()

    fun setDays(list: List<Day>) {
        days = list
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; interpolator = DecelerateInterpolator()
            addUpdateListener { anim = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (days.isEmpty()) return
        val slot = width / days.size.toFloat()
        val ringR = min(slot * 0.32f, height * 0.26f)
        val stroke = ringR * 0.28f
        val ringCy = height * 0.66f
        days.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2
            if (d.isToday) {
                rect.set(cx - slot * 0.46f, 0f, cx + slot * 0.46f, height.toFloat())
                canvas.drawRoundRect(rect, slot * 0.3f, slot * 0.3f, todayBg)
            }
            text.color = if (d.isToday) Color.WHITE else 0x88FFFFFF.toInt()
            canvas.drawText(d.letter, cx, height * 0.26f, text)
            val local = ((anim * 1.6f) - i * 0.08f).coerceIn(0f, 1f)
            painter.draw(canvas, cx, ringCy, ringR, stroke, if (d.future) 0f else d.ratio * local, 0x22FFFFFF, false)
            val emoji = when {
                d.frozen -> "🧊"
                d.ratio >= 1f && !d.future -> "🔥"
                else -> null
            }
            if (emoji != null && local > 0.9f) {
                canvas.drawText(emoji, cx, ringCy + icon.textSize * 0.36f, icon)
            }
        }
    }
}

// ============================================================ Histogramme

class BarChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    data class Bar(val label: String, val value: Int, val key: String)

    var onSelect: ((Int) -> Unit)? = null
    private var bars: List<Bar> = emptyList()
    private var goal = 0
    private var anim = 0f
    var selected = -1
        private set

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x0FFFFFFF }
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = context.dp(1.5f); color = 0x88FFD24D.toInt()
        pathEffect = DashPathEffect(floatArrayOf(context.dp(6f), context.dp(5f)), 0f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = context.dp(11f); color = 0x80FFFFFF.toInt()
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = context.dp(13f); color = Color.WHITE
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    }
    private val goalText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT; textSize = context.dp(10f); color = 0xCCFFD24D.toInt()
    }
    private val rect = RectF()

    fun setData(list: List<Bar>, goal: Int) {
        bars = list; this.goal = goal; selected = list.lastIndex
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100; interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { anim = it.animatedValue as Float; invalidate() }
            start()
        }
        onSelect?.invoke(selected)
    }

    override fun onDraw(canvas: Canvas) {
        if (bars.isEmpty()) return
        val top = context.dp(26f)
        val bottom = context.dp(22f)
        val chartH = height - top - bottom
        val maxV = max(max(goal * 1.15f, (bars.maxOf { it.value }) * 1.08f), 1f)
        val slot = width / bars.size.toFloat()
        val barW = slot * if (bars.size <= 7) 0.52f else 0.64f
        val r = min(barW / 2f, context.dp(10f))

        bars.forEachIndexed { i, b ->
            val local = ((anim * 1.5f) - i * (0.5f / bars.size)).coerceIn(0f, 1f)
            val left = slot * i + (slot - barW) / 2
            rect.set(left, top, left + barW, top + chartH)
            canvas.drawRoundRect(rect, r, r, trackPaint)
            val bh = max(chartH * (b.value / maxV) * local, if (b.value > 0) r * 2 * local else 0f)
            if (bh > 0f) {
                rect.set(left, top + chartH - bh, left + barW, top + chartH)
                val reached = goal in 1..b.value
                val (ca, cb) = if (reached) Palette.YELLOW to Palette.ORANGE else Palette.PINK to Palette.VIOLET
                barPaint.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, ca, cb, Shader.TileMode.CLAMP)
                barPaint.alpha = if (selected == -1 || selected == i) 255 else 140
                canvas.drawRoundRect(rect, r, r, barPaint)
            }
            val showLabel = bars.size <= 7 || i % 5 == (bars.size - 1) % 5
            if (showLabel) {
                labelPaint.color = if (i == selected) Color.WHITE else 0x80FFFFFF.toInt()
                canvas.drawText(b.label, left + barW / 2, height - context.dp(5f), labelPaint)
            }
            if (i == selected && local >= 1f) {
                canvas.drawText(Fmt.compact(b.value.toLong()), left + barW / 2, top + chartH - bh - context.dp(7f), valuePaint)
            }
        }
        if (goal > 0) {
            val gy = top + chartH * (1f - goal / maxV)
            canvas.drawLine(0f, gy, width.toFloat(), gy, goalPaint)
            canvas.drawText("objectif", width.toFloat(), gy - context.dp(4f), goalText)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bars.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val idx = (event.x / (width / bars.size.toFloat())).toInt().coerceIn(0, bars.lastIndex)
                if (idx != selected) {
                    selected = idx
                    haptic()
                    onSelect?.invoke(idx)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> { performClick(); return true }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}

// ======================================================= Calendrier (heatmap)

class HeatmapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    data class Cell(val ratio: Float, val frozen: Boolean, val isToday: Boolean, val future: Boolean)

    companion object {
        val LEVELS = intArrayOf(0x14FFFFFF, 0xFF2E2557.toInt(), 0xFF4B3A99.toInt(), 0xFF7C5CFF.toInt(), 0xFFFF8A3D.toInt())
        const val FROZEN = 0xFF7FD8FF.toInt()

        fun colorFor(c: Cell): Int = when {
            c.frozen -> FROZEN
            c.ratio >= 1f -> LEVELS[4]
            c.ratio >= 0.66f -> LEVELS[3]
            c.ratio >= 0.33f -> LEVELS[2]
            c.ratio > 0f -> LEVELS[1]
            else -> LEVELS[0]
        }
    }

    /** Colonnes = semaines, 7 lignes (lundi -> dimanche). */
    private var columns: List<List<Cell>> = emptyList()
    private var anim = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = context.dp(1.6f); color = Color.WHITE
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dp(10f); color = 0x77FFFFFF }
    private val rect = RectF()
    private val letters = arrayOf("L", "M", "M", "J", "V", "S", "D")

    fun setData(cols: List<List<Cell>>) {
        columns = cols
        requestLayout()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            addUpdateListener { anim = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun geometry(w: Int): Pair<Float, Float> {
        val labelW = context.dp(16f)
        val n = max(columns.size, 1)
        val cell = (w - labelW) / n
        return labelW to cell
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val (_, cell) = geometry(w)
        setMeasuredDimension(w, (cell * 7).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val (labelW, cell) = geometry(width)
        val gap = cell * 0.16f
        val r = cell * 0.28f
        for (row in 0 until 7) {
            if (row % 2 == 0) canvas.drawText(letters[row], 0f, row * cell + cell * 0.7f, label)
        }
        columns.forEachIndexed { ci, col ->
            val local = ((anim * 1.5f) - ci * (0.5f / max(columns.size, 1))).coerceIn(0f, 1f)
            col.forEachIndexed { ri, c ->
                if (c.future) return@forEachIndexed
                val x = labelW + ci * cell
                val y = ri * cell
                val shrink = (1f - local) * cell * 0.3f
                rect.set(x + gap / 2 + shrink, y + gap / 2 + shrink, x + cell - gap / 2 - shrink, y + cell - gap / 2 - shrink)
                paint.color = colorFor(c)
                paint.alpha = (Color.alpha(paint.color) * local).toInt()
                canvas.drawRoundRect(rect, r, r, paint)
                if (c.isToday) canvas.drawRoundRect(rect, r, r, outline)
            }
        }
    }
}
