package com.stepstreak.app

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import java.text.NumberFormat
import java.util.Locale

/** Petits utilitaires de formatage / d'animation partagés par toute l'app. */
object Fmt {
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.FRANCE)

    fun int(n: Int): String = nf.format(n.toLong())
    fun long(n: Long): String = nf.format(n)

    fun km(v: Double): String =
        if (v < 10) String.format(Locale.FRANCE, "%.2f", v)
        else if (v < 100) String.format(Locale.FRANCE, "%.1f", v)
        else nf.format(Math.round(v))

    fun days(n: Int): String = if (n > 1) "$n jours" else "$n jour"

    fun compact(n: Long): String = when {
        n >= 1_000_000 -> String.format(Locale.FRANCE, "%.1f M", n / 1_000_000.0)
        n >= 10_000 -> String.format(Locale.FRANCE, "%.1f k", n / 1_000.0)
        else -> nf.format(n)
    }
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density).toInt()

fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

/** Effet "bouton qu'on enfonce" : léger rétrécissement au toucher, rebond au relâchement. */
fun View.pressable() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(OvershootInterpolator(3f)).start()
        }
        false
    }
}

fun View.haptic() {
    if (StepPrefs.hapticsEnabled(context)) {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
