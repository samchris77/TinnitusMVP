package com.tinnitustracker.mvp

import com.tinnitustracker.mvp.util.Pitch
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.log10

class PitchTest {
    @Test
    fun roundsToFiveBelowAKilohertzAndToTenAbove() {
        assertEquals(735f, Pitch.round(737.4f))
        assertEquals(4240f, Pitch.round(4237.8f))
    }

    @Test
    fun clampsToTheFinderRange() {
        assertEquals(250f, Pitch.clampRound(100f))
        assertEquals(16_000f, Pitch.clampRound(20_000f))
    }

    @Test
    fun sliderPositionRoundTrips() {
        for (hz in listOf(250f, 1000f, 4000f, 16_000f)) {
            assertEquals(hz, Pitch.fromFraction(Pitch.toFraction(hz)), hz * 1e-4f)
        }
    }

    @Test
    fun aSemitoneUpAndDownLandsBackOnTheRoundedPitch() {
        val up = Pitch.clampRound(Pitch.shiftSemitones(4000f, 1))
        assertEquals(4240f, up)
        assertEquals(4000f, Pitch.clampRound(Pitch.shiftSemitones(up, -1)))
    }

    @Test
    fun formatsWithThousandsSeparators() {
        assertEquals("4,000 Hz", Pitch.format(4000f))
        assertEquals("735 Hz", Pitch.format(735f))
    }

    @Test
    fun describesPitchInPlainWords() {
        assertEquals("낮은 음", Pitch.describe(400f))
        assertEquals("중간 음", Pitch.describe(1000f))
        assertEquals("높은 음", Pitch.describe(4000f))
        assertEquals("아주 높은 음", Pitch.describe(9000f))
    }

    @Test
    fun loudnessStartsSilentAndTopsOutAtMinusFifteenDecibels() {
        assertEquals(0f, Pitch.loudnessGain(0))
        assertEquals(-15f, 20 * log10(Pitch.loudnessGain(100)), 0.01f)
        assertEquals(-71.43f, 20 * log10(Pitch.loudnessGain(1)), 0.01f)
    }
}
