package com.tinnitustracker.mvp

import com.tinnitustracker.mvp.audio.BandStop
import com.tinnitustracker.mvp.audio.LoopSource
import com.tinnitustracker.mvp.audio.Loops
import com.tinnitustracker.mvp.audio.Mixer
import com.tinnitustracker.mvp.audio.NoiseSource
import com.tinnitustracker.mvp.audio.StereoNoise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class DspTest {
    private val sampleRate = 48_000

    /** Gain in dB of a 4 kHz band-stop for a steady sine at [hz]. */
    private fun bandStopGainDb(hz: Double): Double {
        val stop = BandStop(sampleRate.toDouble()).apply { setCenter(4000.0) }
        var phase = 0.0
        var sum = 0.0
        val n = sampleRate
        for (i in 0 until n) {
            val y = stop.process(sin(phase))
            phase += 2 * PI * hz / sampleRate
            if (i >= n / 2) sum += y * y       // skip the first half second while the filter settles
        }
        val rms = sqrt(sum / (n / 2))
        return 20 * log10(rms * sqrt(2.0))
    }

    @Test
    fun bandStopCutsTheCentreAndLeavesDistantPitchesAlone() {
        assertTrue("centre", bandStopGainDb(4000.0) < -25)
        val halfOctave = bandStopGainDb(4000.0 / sqrt(2.0))
        assertTrue("half an octave out: $halfOctave", halfOctave < -10 && halfOctave > -20)
        assertTrue("two octaves below", bandStopGainDb(1000.0) > -1)
        assertTrue("1.6 octaves above", bandStopGainDb(12_000.0) > -2)
    }

    @Test
    fun allThreeNoiseColoursPlayAtAboutTheSameLevel() {
        for (kind in NoiseSource.Kind.entries) {
            val noise = NoiseSource(kind, 42)
            val n = sampleRate * 10
            var sum = 0.0
            repeat(n) {
                val v = noise.next()
                sum += v * v
            }
            assertEquals(kind.name, 0.05, sqrt(sum / n), 0.008)
        }
    }

    @Test
    fun seamlessLoopStartsWhereItsEndLeftOff() {
        val samples = ShortArray(1000) { (it * 7).toShort() }
        val loop = Loops.seamless(samples, fadeFrames = 100, channels = 1)
        assertEquals(900, loop.size)
        assertEquals(samples[900], loop[0])
        assertEquals(samples[899], loop[899])
    }

    @Test
    fun stereoLoopCrossfadesEachChannelOnItsOwn() {
        // Left counts up, right counts down; the seam must keep them apart.
        val frames = 1000
        val samples = ShortArray(frames * 2) { i -> if (i % 2 == 0) (i / 2 * 7).toShort() else (-(i / 2) * 5).toShort() }
        val loop = Loops.seamless(samples, fadeFrames = 100)
        assertEquals(900 * 2, loop.size)
        assertEquals(samples[900 * 2], loop[0])          // left, first frame = the frame after the last
        assertEquals(samples[900 * 2 + 1], loop[1])      // right
        assertEquals(samples[899 * 2 + 1], loop[899 * 2 + 1])
    }

    @Test
    fun loopSourcePlaysFramesInOrderAndWraps() {
        val src = LoopSource(shortArrayOf(1, 2, 3, 4))
        val f = DoubleArray(2)
        val seen = List(3) { src.next(f); (f[0] * 32768).toInt() to (f[1] * 32768).toInt() }
        assertEquals(listOf(1 to 2, 3 to 4, 1 to 2), seen)
    }

    @Test
    fun stereoNoiseHasTwoIndependentChannelsAtTheUsualLevel() {
        val noise = StereoNoise(NoiseSource.Kind.PINK, 11)
        val f = DoubleArray(2)
        val n = sampleRate * 5
        var ll = 0.0
        var rr = 0.0
        var lr = 0.0
        repeat(n) {
            noise.next(f)
            ll += f[0] * f[0]
            rr += f[1] * f[1]
            lr += f[0] * f[1]
        }
        assertEquals(0.05, sqrt(ll / n), 0.008)
        assertEquals(0.05, sqrt(rr / n), 0.008)
        val correlation = lr / sqrt(ll * rr)
        assertTrue("left and right correlate at $correlation", kotlin.math.abs(correlation) < 0.1)
    }

    @Test
    fun probeToneHasTheRequestedLevelAndTheMixerGoesIdleAfter() {
        val mixer = Mixer(sampleRate)
        mixer.probeHz = 1000.0
        mixer.probeLevel = 0.1
        mixer.probe = Mixer.Probe.TONE
        val block = FloatArray(4800 * 2)
        repeat(10) { mixer.render(block) }
        val rms = sqrt(block.sumOf { it.toDouble() * it } / block.size)
        assertEquals(0.1 / sqrt(2.0), rms, 0.002)
        assertEquals("the tone sits in the centre", block[4000], block[4001])

        mixer.probe = Mixer.Probe.OFF
        repeat(10) { mixer.render(block) }
        assertTrue(mixer.isIdle)
    }

    @Test
    fun notchTakesThePitchOutOfPinkNoise() {
        fun energyAt4k(notch: Boolean): Double {
            val mixer = Mixer(sampleRate)
            mixer.setSource(StereoNoise(NoiseSource.Kind.PINK, 7))
            mixer.notchHz = 4000.0
            mixer.notchOn = notch
            mixer.therapyOn = true
            val block = FloatArray(4800 * 2)
            repeat(5) { mixer.render(block) }
            var energy = 0.0
            repeat(20) {
                mixer.render(block)
                val left = FloatArray(4800) { block[2 * it] }
                val right = FloatArray(4800) { block[2 * it + 1] }
                energy += goertzel(left, 4000.0) + goertzel(right, 4000.0)
            }
            return energy
        }
        val changeDb = 10 * log10(energyAt4k(notch = true) / energyAt4k(notch = false))
        assertTrue("notch only changed 4 kHz by $changeDb dB", changeDb < -20)
    }

    @Test
    fun sleepTimerFadeReachesSilenceAndComesBack() {
        val mixer = Mixer(sampleRate)
        mixer.setSource(StereoNoise(NoiseSource.Kind.WHITE, 3))
        mixer.therapyOn = true
        val block = FloatArray(4800 * 2)
        repeat(5) { mixer.render(block) }
        fun rms() = sqrt(block.sumOf { it.toDouble() * it } / block.size)
        assertEquals(0.05, rms(), 0.01)

        mixer.fadeTo(0.0, 0.2)
        repeat(3) { mixer.render(block) }   // 0.3 s
        assertEquals(0.0, rms(), 1e-9)

        mixer.fadeTo(1.0, 0.0)
        mixer.render(block)
        assertEquals(0.05, rms(), 0.01)
    }

    /** Power of one frequency in a block. */
    private fun goertzel(x: FloatArray, hz: Double): Double {
        val c = 2 * cos(2 * PI * hz / sampleRate)
        var s1 = 0.0
        var s2 = 0.0
        for (v in x) {
            val s0 = v + c * s1 - s2
            s2 = s1
            s1 = s0
        }
        return s1 * s1 + s2 * s2 - c * s1 * s2
    }
}
