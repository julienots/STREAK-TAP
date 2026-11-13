package com.stepstreak.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max
import kotlin.random.Random

/** Rendus statiques (bitmaps) pour les widgets et notifications, qui n'acceptent pas de vues perso. */
object BitmapRenderer {

    fun ring(size: Int, progress: Float, trackColor: Int = 0x26FFFFFF, strokeRatio: Float = 0.1f, glow: Boolean = true): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val stroke = size * strokeRatio
        val radius = size / 2f - stroke * (if (glow) 1.1f else 0.55f)
        val tip = RingPainter().draw(c, size / 2f, size / 2f, radius, stroke, progress, trackColor, glow)
        if (progress > 0.01f) {
            val a = Math.toRadians(tip.toDouble())
            val x = size / 2f + (radius * Math.cos(a)).toFloat()
            val y = size / 2f + (radius * Math.sin(a)).toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            p.alpha = 70
            c.drawCircle(x, y, stroke * 0.8f, p)
            p.alpha = 255
            c.drawCircle(x, y, stroke * 0.3f, p)
        }
        return bmp
    }

    fun flame(size: Int, active: Boolean, t: Float = 0.4f): Bitmap {
        val bmp = Bitmap.createBitmap(size, (size * 1.15f).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        FlamePainter().draw(c, size * 0.06f, size * 0.06f, size * 0.88f, bmp.height - size * 0.12f, t, active)
        return bmp
    }

    /** Mini-histogramme des 7 derniers jours pour le grand widget. */
    fun weekBars(w: Int, h: Int, values: List<Int>, goal: Int, letters: List<String>): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (values.isEmpty()) return bmp
        val labelH = h * 0.32f
        val chartH = h - labelH
        val slot = w / values.size.toFloat()
        val barW = slot * 0.42f
        val r = barW / 2f
        val maxV = max(max(goal.toFloat(), values.maxOrNull()?.toFloat() ?: 0f), 1f)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = labelH * 0.72f; color = 0x99FFFFFF.toInt()
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val rect = RectF()
        values.forEachIndexed { i, v ->
            val left = slot * i + (slot - barW) / 2
            rect.set(left, 0f, left + barW, chartH)
            c.drawRoundRect(rect, r, r, track)
            val bh = max(chartH * (v / maxV), if (v > 0) barW else 0f).coerceAtMost(chartH)
            if (bh > 0) {
                rect.set(left, chartH - bh, left + barW, chartH)
                val reached = goal in 1..v
                bar.shader = LinearGradient(0f, rect.top, 0f, rect.bottom,
                    if (reached) Palette.YELLOW else Palette.PINK,
                    if (reached) Palette.ORANGE else Palette.VIOLET, Shader.TileMode.CLAMP)
                c.drawRoundRect(rect, r, r, bar)
            }
            text.color = if (i == values.lastIndex) Color.WHITE else 0x88FFFFFF.toInt()
            c.drawText(letters.getOrElse(i) { "" }, left + barW / 2, h - labelH * 0.12f, text)
        }
        return bmp
    }

    /** Carte de célébration (BigPicture des notifications). */
    fun celebration(title: String, subtitle: String, emoji: String, colorA: Int, colorB: Int): Bitmap {
        val w = 1024
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colorA, colorB, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        val dark = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0x00000000, 0x77000000, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dark)
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(w / 2f, h * 0.38f, h * 0.55f, 0x88FFFFFF.toInt(), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawCircle(w / 2f, h * 0.38f, h * 0.55f, halo)

        // confettis figés
        val rnd = Random(title.hashCode())
        val confetti = Paint(Paint.ANTI_ALIAS_FLAG)
        val colors = intArrayOf(Color.WHITE, Palette.YELLOW, Palette.MINT, Palette.PINK, Palette.ICE)
        repeat(70) {
            confetti.color = colors[rnd.nextInt(colors.size)]
            confetti.alpha = 120 + rnd.nextInt(135)
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            c.save()
            c.rotate(rnd.nextFloat() * 360f, x, y)
            if (rnd.nextBoolean()) c.drawRect(x, y, x + 10f + rnd.nextFloat() * 8f, y + 18f + rnd.nextFloat() * 10f, confetti)
            else c.drawCircle(x, y, 4f + rnd.nextFloat() * 5f, confetti)
            c.restore()
        }

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 170f; textAlign = Paint.Align.CENTER }
        c.drawText(emoji, w / 2f, h * 0.48f, emojiPaint)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 62f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            setShadowLayer(12f, 0f, 4f, 0x66000000)
        }
        c.drawText(title, w / 2f, h * 0.72f, titlePaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt(); textSize = 36f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        c.drawText(subtitle, w / 2f, h * 0.85f, subPaint)
        return bmp
    }

    /** Pastille ronde avec emoji (grande icône des notifications). */
    fun emojiBadge(emoji: String, colorA: Int, colorB: Int, size: Int = 192): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colorA, colorB, Shader.TileMode.CLAMP)
        }
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size * 0.52f; textAlign = Paint.Align.CENTER }
        c.drawText(emoji, size / 2f, size / 2f + t.textSize * 0.36f, t)
        return bmp
    }
}
