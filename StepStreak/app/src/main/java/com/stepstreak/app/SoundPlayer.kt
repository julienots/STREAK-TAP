package com.stepstreak.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/** Sons joués dans l'app (les notifications utilisent les sons de leur canal). */
object SoundPlayer {
    fun playGoalReached(context: Context) = play(context, R.raw.chime_success)
    fun playBadgeUnlocked(context: Context) = play(context, R.raw.chime_badge)
    fun playNudge(context: Context) = play(context, R.raw.chime_nudge)

    private fun play(context: Context, resId: Int) {
        if (!StepPrefs.soundsEnabled(context)) return
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = context.resources.openRawResourceFd(resId)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { player, _, _ -> player.release(); true }
            mp.prepare()
            mp.start()
        } catch (e: Exception) { }
    }
}
