package com.tinnitustracker.mvp.audio

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Everything the listener hears, one block of interleaved stereo at a time: the chosen sound
 * (cross-faded when it changes), the optional band-stop at the tinnitus pitch on each channel,
 * the play/pause fade, and the finder's probe tone or hiss, which sit in the centre. Pure
 * Kotlin so it can be tested off-device.
 *
 * The control fields are written from the main thread and read once per block on the audio
 * thread; every change is smoothed, so nothing clicks.
 */
class Mixer(sampleRate: Int) {
    enum class Probe { OFF, TONE, NOISE }

    @Volatile var therapyOn = false
    @Volatile var notchOn = false
    @Volatile var notchHz = 4000.0
    @Volatile var probe = Probe.OFF
    @Volatile var probeHz = 1000.0
    @Volatile var probeLevel = 0.0

    private val sr = sampleRate.toDouble()
    private val pending = AtomicReference<FrameSource?>(null)

    private class Voice(val source: FrameSource, val gain: Smoothed)

    private var current: Voice? = null
    private var fading: Voice? = null

    private val bandStopL = BandStop(sr)
    private val bandStopR = BandStop(sr)
    private val frame = DoubleArray(2)
    private val dry = Smoothed(1.0, sr, 0.03)
    private val wet = Smoothed(0.0, sr, 0.03)
    private val therapyGain = Smoothed(0.0, sr, 0.06)
    private val toneGain = Smoothed(0.0, sr, 0.03)
    private val hissGain = Smoothed(0.0, sr, 0.03)
    private val toneHz = Smoothed(1000.0, sr, 0.01)
    private val hiss = NoiseSource(NoiseSource.Kind.WHITE, 0x5EED)
    private var phase = 0.0

    // Sleep-timer fade: a linear ramp on the chosen sound only.
    private val fadeCommand = AtomicReference<DoubleArray?>(null)
    private var fadeGain = 1.0
    private var fadeTarget = 1.0
    private var fadeStep = 0.0

    /** Switches to [source] with a short crossfade. Safe from any thread. */
    fun setSource(source: FrameSource) {
        pending.set(source)
    }

    /** Ramps the chosen sound linearly to [target] (0–1) over [seconds]; 0 seconds is immediate. Safe from any thread. */
    fun fadeTo(target: Double, seconds: Double) {
        fadeCommand.set(doubleArrayOf(target, seconds))
    }

    /** True once everything has faded out. Call from the audio thread. */
    val isIdle: Boolean
        get() = !therapyOn && probe == Probe.OFF &&
            therapyGain.isSilent && toneGain.isSilent && hissGain.isSilent

    /** Fills [out] with [frames] interleaved stereo frames (left, right, left, …). */
    fun render(out: FloatArray, frames: Int = out.size / 2) {
        pending.getAndSet(null)?.let { source ->
            fading = current?.also { it.gain.target = 0.0 }
            current = Voice(source, Smoothed(0.0, sr, CROSSFADE).also { it.target = 1.0 })
        }
        fadeCommand.getAndSet(null)?.let { (target, seconds) ->
            fadeTarget = target.coerceIn(0.0, 1.0)
            val samples = seconds * sr
            fadeStep = if (samples < 1) Double.POSITIVE_INFINITY else abs(fadeTarget - fadeGain) / samples
        }
        bandStopL.setCenter(notchHz)
        bandStopR.setCenter(notchHz)
        val notch = notchOn
        dry.target = if (notch) 0.0 else 1.0
        wet.target = if (notch) 1.0 else 0.0
        therapyGain.target = if (therapyOn) 1.0 else 0.0
        val p = probe
        toneGain.target = if (p == Probe.TONE) probeLevel else 0.0
        hissGain.target = if (p == Probe.NOISE) probeLevel * HISS_BOOST else 0.0
        toneHz.target = probeHz
        if (toneGain.value < Smoothed.SILENT) toneHz.jump()   // no glide into a tone that is starting

        val cur = current
        val fade = fading
        val therapyActive = !therapyGain.isSilent
        val toneActive = !toneGain.isSilent
        val hissActive = !hissGain.isSilent
        for (i in 0 until frames) {
            var left = 0.0
            var right = 0.0
            if (therapyActive) {
                var l = 0.0
                var r = 0.0
                if (cur != null) {
                    cur.source.next(frame)
                    val g = cur.gain.next()
                    l += frame[0] * g
                    r += frame[1] * g
                }
                if (fade != null) {
                    fade.source.next(frame)
                    val g = fade.gain.next()
                    l += frame[0] * g
                    r += frame[1] * g
                }
                val notchedL = bandStopL.process(l)
                val notchedR = bandStopR.process(r)
                if (fadeGain != fadeTarget) {
                    fadeGain = if (fadeGain > fadeTarget) max(fadeTarget, fadeGain - fadeStep) else min(fadeTarget, fadeGain + fadeStep)
                }
                val d = dry.next()
                val w = wet.next()
                val g = therapyGain.next() * fadeGain
                left += (l * d + notchedL * w) * g
                right += (r * d + notchedR * w) * g
            }
            var centre = 0.0
            if (toneActive) {
                phase += TWO_PI * toneHz.next() / sr
                if (phase >= TWO_PI) phase -= TWO_PI
                centre += sin(phase) * toneGain.next()
            }
            if (hissActive) centre += hiss.next() * hissGain.next()
            out[2 * i] = (left + centre).coerceIn(-1.0, 1.0).toFloat()
            out[2 * i + 1] = (right + centre).coerceIn(-1.0, 1.0).toFloat()
        }
        if (fade != null && fade.gain.value < Smoothed.SILENT) fading = null
    }

    private companion object {
        const val TWO_PI = 2 * PI
        const val CROSSFADE = 0.08
        /** The hiss (0.05 RMS) is boosted so it sits near a tone at the same slider position. */
        const val HISS_BOOST = 10.0
    }
}
