package com.tinnitustracker.mvp.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Anything that produces one sample at a time. */
fun interface SampleSource {
    fun next(): Double
}

/** Anything that produces one stereo frame at a time: left into out[0], right into out[1]. */
fun interface FrameSource {
    fun next(out: DoubleArray)
}

/** Two independent noise generators, one per ear, so the noise sounds wide rather than inside the head. */
class StereoNoise(kind: NoiseSource.Kind, seed: Long) : FrameSource {
    private val left = NoiseSource(kind, seed)
    private val right = NoiseSource(kind, seed * GOLDEN + 0x632BE5AB)

    private companion object {
        /** 2^64 / golden ratio, as a signed Long: scatters the second seed far from the first. */
        const val GOLDEN = -7046029254386353131L
    }

    override fun next(out: DoubleArray) {
        out[0] = left.next()
        out[1] = right.next()
    }
}

/** RBJ-cookbook biquad in transposed direct form II. */
class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var z1 = 0.0
    private var z2 = 0.0

    fun setLowPass(sampleRate: Double, hz: Double, q: Double = BUTTERWORTH_Q) {
        val w0 = 2 * PI * hz / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2 * q)
        val a0 = 1 + alpha
        b0 = (1 - cosW) / 2 / a0
        b1 = (1 - cosW) / a0
        b2 = b0
        a1 = -2 * cosW / a0
        a2 = (1 - alpha) / a0
    }

    fun setHighPass(sampleRate: Double, hz: Double, q: Double = BUTTERWORTH_Q) {
        val w0 = 2 * PI * hz / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2 * q)
        val a0 = 1 + alpha
        b0 = (1 + cosW) / 2 / a0
        b1 = -(1 + cosW) / a0
        b2 = b0
        a1 = -2 * cosW / a0
        a2 = (1 - alpha) / a0
    }

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    companion object {
        val BUTTERWORTH_Q = 1 / sqrt(2.0)
    }
}

/**
 * About one octave of band-stop centred on the tinnitus pitch: a low-pass branch with its corner
 * 0.6 octave below the centre plus a high-pass branch 0.6 octave above, each four 2nd-order
 * sections. Roughly −30 dB at the centre and −15 dB half an octave either side.
 */
class BandStop(private val sampleRate: Double) {
    private val low = Array(SECTIONS) { Biquad() }
    private val high = Array(SECTIONS) { Biquad() }
    private var centerHz = Double.NaN

    fun setCenter(hz: Double) {
        if (hz == centerHz) return
        centerHz = hz
        val ceiling = sampleRate / 2 * 0.95
        val lowCorner = min(ceiling, hz * 2.0.pow(-CORNER_OCTAVES))
        val highCorner = min(ceiling, hz * 2.0.pow(CORNER_OCTAVES))
        low.forEach { it.setLowPass(sampleRate, lowCorner) }
        high.forEach { it.setHighPass(sampleRate, highCorner) }
    }

    fun process(x: Double): Double {
        var l = x
        for (f in low) l = f.process(l)
        var h = x
        for (f in high) h = f.process(h)
        return l + h
    }

    private companion object {
        const val SECTIONS = 4
        const val CORNER_OCTAVES = 0.6
    }
}

/** One-pole smoother for gains and frequencies, advanced once per sample. */
class Smoothed(initial: Double, sampleRate: Double, timeConstantSeconds: Double) {
    private val k = 1 - exp(-1 / (timeConstantSeconds * sampleRate))
    var target = initial
    var value = initial
        private set

    fun next(): Double {
        value += (target - value) * k
        return value
    }

    fun jump() {
        value = target
    }

    val isSilent: Boolean get() = target == 0.0 && abs(value) < SILENT

    companion object {
        const val SILENT = 1e-5
    }
}

/** White, pink (Paul Kellet's filter, −3 dB/octave) or brown (leaky integrator, −6 dB/octave) noise, all near 0.05 RMS. */
class NoiseSource(private val kind: Kind, seed: Long) : SampleSource {
    enum class Kind { WHITE, PINK, BROWN }

    private var state = seed or 1L
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0
    private var brown = 0.0

    override fun next(): Double {
        val w = white()
        return when (kind) {
            Kind.WHITE -> w * WHITE_SCALE
            Kind.PINK -> {
                b0 = 0.99886 * b0 + w * 0.0555179
                b1 = 0.99332 * b1 + w * 0.0750759
                b2 = 0.96900 * b2 + w * 0.1538520
                b3 = 0.86650 * b3 + w * 0.3104856
                b4 = 0.55000 * b4 + w * 0.5329522
                b5 = -0.7616 * b5 - w * 0.0168980
                val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
                b6 = w * 0.115926
                pink * PINK_SCALE
            }
            Kind.BROWN -> {
                brown = (brown + 0.02 * w) / 1.02
                brown * BROWN_SCALE
            }
        }
    }

    /** Uniform in [−1, 1) from xorshift64. */
    private fun white(): Double {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return (x ushr 11) * INV_2_53 * 2 - 1
    }

    companion object {
        // Measured RMS of each raw generator: white 0.577, Kellet pink 1.745, this brown 0.0573.
        const val WHITE_SCALE = 0.0866
        const val PINK_SCALE = 0.02865
        const val BROWN_SCALE = 0.873
        private const val INV_2_53 = 1.0 / (1L shl 53)
    }
}

/** Plays a 16-bit interleaved stereo buffer round and round. Build the buffer with [Loops.seamless]. */
class LoopSource(private val data: ShortArray) : FrameSource {
    private var pos = 0

    init {
        require(data.size % 2 == 0 && data.isNotEmpty()) { "Expected interleaved stereo, got ${data.size} samples" }
    }

    override fun next(out: DoubleArray) {
        out[0] = data[pos] * INV_32768
        out[1] = data[pos + 1] * INV_32768
        pos += 2
        if (pos == data.size) pos = 0
    }

    private companion object {
        const val INV_32768 = 1.0 / 32768
    }
}

object Loops {
    /**
     * Cross-fades the last [fadeFrames] frames into the start (equal power) and drops them, so
     * the result loops without a seam: its first frame is the one that followed its last.
     * [samples] holds [channels] interleaved channels.
     */
    fun seamless(samples: ShortArray, fadeFrames: Int, channels: Int = 2): ShortArray {
        require(channels > 0 && samples.size % channels == 0) { "${samples.size} samples is not whole $channels-channel frames" }
        val frames = samples.size / channels
        val length = frames - fadeFrames
        require(fadeFrames > 0 && length > fadeFrames) { "Need more than twice $fadeFrames frames, got $frames" }
        val out = samples.copyOf(length * channels)
        for (i in 0 until fadeFrames) {
            val t = i.toDouble() / fadeFrames
            val a = sqrt(t)
            val b = sqrt(1 - t)
            for (c in 0 until channels) {
                val mixed = samples[i * channels + c] * a + samples[(length + i) * channels + c] * b
                out[i * channels + c] = mixed.roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return out
    }
}
