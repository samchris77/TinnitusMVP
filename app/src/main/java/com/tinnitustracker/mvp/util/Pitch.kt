package com.tinnitustracker.mvp.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** Pitch and loudness maths shared by the finder, the main screen and the notification. */
object Pitch {
    const val MIN_HZ = 250f
    const val MAX_HZ = 16_000f
    const val START_HZ = 2_000f

    /** Fixed, quiet level for the pitch and octave steps (−32 dBFS). */
    val TONE_LEVEL = dbToGain(-32f)

    /** Nearest 5 Hz below 1 kHz, nearest 10 Hz above. */
    fun round(hz: Float): Float =
        if (hz < 1000f) (hz / 5f).roundToInt() * 5f else (hz / 10f).roundToInt() * 10f

    fun clampRound(hz: Float): Float = round(hz).coerceIn(MIN_HZ, MAX_HZ)

    fun inRange(hz: Float): Boolean = hz in MIN_HZ..MAX_HZ

    /** Position on the log-scaled pitch slider, 0–1. */
    fun toFraction(hz: Float): Float = (ln(hz / MIN_HZ) / ln(MAX_HZ / MIN_HZ)).coerceIn(0f, 1f)

    fun fromFraction(fraction: Float): Float = MIN_HZ * (MAX_HZ / MIN_HZ).pow(fraction.coerceIn(0f, 1f))

    fun shiftSemitones(hz: Float, semitones: Int): Float = hz * 2f.pow(semitones / 12f)

    fun shiftOctaves(hz: Float, octaves: Int): Float = hz * 2f.pow(octaves)

    fun format(hz: Float): String =
        NumberFormat.getIntegerInstance(Locale.KOREA).format(hz.roundToInt()) + " Hz"

    fun describe(hz: Float): String = when {
        hz < 500f -> "낮은 음"
        hz < 2000f -> "중간 음"
        hz < 8000f -> "높은 음"
        else -> "아주 높은 음"
    }

    /** Loudness slider 0–100 → probe amplitude: 0 is silent, 1 ≈ −71 dBFS, 100 = −15 dBFS. */
    fun loudnessGain(value: Int): Float = if (value <= 0) 0f else dbToGain(-72f + 0.57f * value)

    fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
}
