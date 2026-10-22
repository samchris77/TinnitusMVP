package com.tinnitustracker.mvp

import com.tinnitustracker.mvp.data.CheckIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CheckInTest {
    private val oct21 = LocalDate.of(2026, 10, 21)

    @Test
    fun scoreUsesTheValidatedKtmiWeights() {
        assertEquals(48, CheckIn(oct21, q1 = 5, q2 = 70, q3 = 42).score)
        assertEquals(0, CheckIn(oct21, 0, 0, 0).score)
        assertEquals(100, CheckIn(oct21, 10, 100, 100).score)
    }

    @Test
    fun encodesAndDecodesInDateOrder() {
        val list = listOf(CheckIn(oct21, 5, 70, 42), CheckIn(oct21.minusDays(7), 6, 70, 48))
        val back = CheckIn.decode(CheckIn.encode(list))
        assertEquals(list.sortedBy { it.date }, back)
    }

    @Test
    fun decodeSkipsDamagedEntries() {
        assertEquals(listOf(CheckIn(oct21, 5, 70, 42)), CheckIn.decode("2026-10-21,5,70,42;oops;2026-13-40,1,2,3"))
        assertEquals(emptyList<CheckIn>(), CheckIn.decode(null))
    }

    @Test
    fun aSecondCheckInOnTheSameDayReplacesTheFirst() {
        val list = CheckIn.add(listOf(CheckIn(oct21, 5, 70, 42)), CheckIn(oct21, 4, 60, 30))
        assertEquals(listOf(CheckIn(oct21, 4, 60, 30)), list)
    }

    @Test
    fun dueOncePerWeekAndRightAwayTheFirstTime() {
        assertTrue(CheckIn.isDue(emptyList(), oct21))
        val list = listOf(CheckIn(oct21, 5, 70, 42))
        assertFalse(CheckIn.isDue(list, oct21.plusDays(6)))
        assertTrue(CheckIn.isDue(list, oct21.plusDays(7)))
        assertEquals(oct21.plusDays(7), CheckIn.nextDue(list, oct21))
    }
}
