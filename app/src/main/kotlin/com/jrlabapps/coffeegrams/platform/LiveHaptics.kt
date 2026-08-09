package com.jrlabapps.coffeegrams.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jrlabapps.coffeegrams.core.Haptics

/**
 * The live [Haptics] adapter, backed by [Vibrator].
 *
 * The three cues are told apart by duration/pattern rather than amplitude:
 * many devices report no amplitude control and silently fall back to a
 * default strength, which would make [targetReached] and [finished]
 * indistinguishable on that hardware.
 */
class LiveHaptics(context: Context) : Haptics {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun stepChange() {
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun targetReached() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun finished() {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
    }
}
