package com.stepstreak.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.sin

object Palette {
    const val VIOLET = 0xFF7C5CFF.toInt()
    const val PINK = 0xFFFF4FA3.toInt()
    const val ORANGE = 0xFFFF8A3D.toInt()
    const val YELLOW = 0xFFFFD24D.toInt()
    const val MINT = 0xFF3DF5B0.toInt()
    const val ICE = 0xFF7FD8FF.toInt()
    const val BG = 0xFF0B0A14.toInt()

    val RING_MAIN = intArrayOf(VIOLET, PINK, ORANGE, YELLOW, VIOLET)
    val RING_OVER = intArrayOf(MINT, ICE, MINT)
}

/** Dessine l'anneau de progression (utilisé par la vue, le widget et la notification). */
class RingPainter {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val rect = RectF()
    private var mainShader: SweepGradient? = null
    private var overShader: SweepGradient? = null
    private var lastCx = -1f
    private var lastCy = -1f

    private fun ensureShaders(cx: Float, cy: Float) {
        if (cx == lastCx && cy == lastCy && mainShader != null) return
        val m = Matrix().apply { setRotate(-90f, cx, cy) }
        mainShader = SweepGradient(cx, cy, Palette.RING_MAIN, null).apply { setLocalMatrix(m) }
        overShader = SweepGradient(cx, cy, Palette.RING_OVER, null).apply { setLocalMatrix(m) }
        lastCx = cx; lastCy = cy
    }

    /** Retourne la position (angle en degrés) de l'extrémité de l'arc. */
    fun draw(
        canvas: Canvas, cx: Float, cy: Float, radius: Float, stroke: Float,
        progress: Float, trackColor: Int, withGlow: Boolean
    ): Float {
        ensureShaders(cx, cy)
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        track.strokeWidth = stroke
        track.color = trackColor
        canvas.drawCircle(cx, cy, radius, track)

        val p1 = progress.coerceIn(0f, 1f)
        if (p1 > 0.001f) {
            if (withGlow) {
                glow.shader = mainShader
                glow.strokeWidth = stroke * 2.1f; glow.alpha = 34
                canvas.drawArc(rect, -90f, 360f * p1, false, glow)
                glow.strokeWidth = stroke * 1.45f; glow.alpha = 60
                canvas.drawArc(rect, -90f, 360f * p1, false, glow)
            }
            arc.shader = mainShader
            arc.strokeWidth = stroke
            canvas.drawArc(rect, -90f, 360f * p1, false, arc)
        }
        var tip = -90f + 360f * p1
        if (progress > 1f) {
            val p2 = (progress - 1f).coerceAtMost(1f)
            arc.shader = overShader
            arc.strokeWidth = stroke * 0.55f
            canvas.drawArc(rect, -90f, 360f * p2, false, arc)
            tip = -90f + 360f * p2
        }
        return tip
    }
}

/** Dessine une flamme vivante en 3 couches (rouge, orange, cœur jaune). */
class FlamePainter {
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val activeLayers = arrayOf(
        intArrayOf(0xFFFF2E4D.toInt(), 0xFFFF7A1A.toInt()),
        intArrayOf(0xFFFF8A1A.toInt(), 0xFFFFC233.toInt()),
        intArrayOf(0xFFFFF4B8.toInt(), 0xFFFFE066.toInt())
    )
    private val inactiveLayers = arrayOf(
        intArrayOf(0xFF4A4560.toInt(), 0xFF5B5575.toInt()),
        intArrayOf(0xFF6A6488.toInt(), 0xFF7A749A.toInt()),
        intArrayOf(0xFF9A94B8.toInt(), 0xFF8A84A8.toInt())
    )
    private val scales = floatArrayOf(1f, 0.70f, 0.42f)

    fun draw(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, t: Float, active: Boolean) {
        val cx = left + w / 2f
        val bottom = top + h
        if (active) {
            glowPaint.shader = RadialGradient(
                cx, bottom - h * 0.32f, w * 0.75f,
                0x66FF7A1A, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, bottom - h * 0.32f, w * 0.75f, glowPaint)
        }
        val layers = if (active) activeLayers else inactiveLayers
        val amp = if (active) 1f else 0.25f
        for (i in 0 until 3) {
            val s = scales[i]
            val flick = 1f + amp * (0.07f * sin(t * 7.3f + i * 1.7f) + 0.035f * sin(t * 12.9f + i))
            val fh = h * 0.96f * s * flick
            val fw = w * 0.86f * s
            val layerBottom = bottom - h * 0.03f - (1f - s) * h * 0.06f
            val sway = amp * (0.45f * sin(t * 3.1f + i * 0.8f) + 0.2f * sin(t * 5.7f + i * 2.1f))
            buildPath(cx, layerBottom, fw, fh, sway)
            paint.shader = LinearGradient(
                0f, layerBottom - fh, 0f, layerBottom,
                layers[i][0], layers[i][1], Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, paint)
        }
    }

    private fun buildPath(cx: Float, bottom: Float, fw: Float, fh: Float, sway: Float) {
        val top = bottom - fh
        val tipX = cx + sway * fw * 0.28f
        path.reset()
        path.moveTo(tipX, top)
        path.cubicTo(tipX + fw * 0.10f, top + fh * 0.24f, cx + fw * 0.54f, top + fh * 0.40f, cx + fw * 0.48f, top + fh * 0.68f)
        path.cubicTo(cx + fw * 0.45f, top + fh * 0.91f, cx + fw * 0.24f, bottom, cx, bottom)
        path.cubicTo(cx - fw * 0.24f, bottom, cx - fw * 0.45f, top + fh * 0.91f, cx - fw * 0.48f, top + fh * 0.68f)
        path.cubicTo(cx - fw * 0.54f, top + fh * 0.40f, tipX - fw * 0.10f, top + fh * 0.24f, tipX, top)
        path.close()
    }
}
