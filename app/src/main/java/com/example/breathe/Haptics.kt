package com.example.breathe

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic language, so you never need audio:
 *  - INHALE: taps start sparse and faint, then get faster and stronger, ending in one firm "full" tap.
 *  - EXHALE: taps start dense and strong, then thin out and fade, ending in a barely-there tick.
 * On motors without amplitude control the same shape is carried by tap spacing and length alone.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val hasAmplitude = vibrator.hasAmplitudeControl()

    /** 1 = soft, 2 = medium, 3 = firm. Scales amplitude and tap length (length matters on motors without amplitude control). */
    var strength: Int = 2
    private val ampScale get() = when (strength) { 1 -> 0.6f; 3 -> 1f; else -> 0.85f }
    private val lenScale get() = when (strength) { 1 -> 0.8f; 3 -> 1.3f; else -> 1f }

    fun inhale(durationMs: Long) = play(ramp(durationMs, rising = true))
    fun exhale(durationMs: Long) = play(ramp(durationMs, rising = false))

    /** Two soft taps and a firmer one: "session complete". */
    fun done() = play(
        waveform(
            longArrayOf(0, 40, 120, 40, 120, 90),
            intArrayOf(0, 120, 0, 120, 0, 220)
        )
    )

    fun cancel() = vibrator.cancel()

    private fun play(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) vibrator.vibrate(effect)
    }

    // Timings alternate off/on, starting with "off". Amplitudes line up 1:1 with timings.
    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect =
        if (hasAmplitude) VibrationEffect.createWaveform(timings, amplitudes, -1)
        else VibrationEffect.createWaveform(timings, -1)

    private fun ramp(durationMs: Long, rising: Boolean): VibrationEffect {
        val timings = ArrayList<Long>()
        val amplitudes = ArrayList<Int>()
        var elapsed = 0L
        while (elapsed < durationMs - 250) {
            val frac = elapsed.toFloat() / durationMs
            val k = if (rising) frac else 1f - frac       // 0 = faint/sparse, 1 = strong/dense
            val gap = lerp(260f, 80f, k).toLong()
            val on = (lerp(14f, 34f, k) * lenScale).toLong()
            val amp = (lerp(40f, 255f, k) * ampScale).toInt().coerceIn(1, 255)
            timings += gap; amplitudes += 0
            timings += on; amplitudes += amp
            elapsed += gap + on
        }
        if (rising) {                                       // top-of-breath marker
            timings += 90; amplitudes += 0
            timings += (70 * lenScale).toLong(); amplitudes += (255 * ampScale).toInt()
        } else {                                            // bottom-of-breath tick
            timings += 120; amplitudes += 0
            timings += (30 * lenScale).toLong(); amplitudes += (60 * ampScale).toInt().coerceAtLeast(1)
        }
        return waveform(timings.toLongArray(), amplitudes.toIntArray())
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
