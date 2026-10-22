package com.tinnitustracker.mvp

import com.tinnitustracker.mvp.data.ListeningLog
import com.tinnitustracker.mvp.data.ListeningLog.Entry
import com.tinnitustracker.mvp.data.ListeningLog.Shade
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class ListeningLogTest {
    private val oct18 = LocalDate.of(2026, 10, 18)
    private val oct = YearMonth.of(2026, 10)

    @Test
    fun encodesAndDecodesInDayOrder() {
        val list = listOf(Entry(oct18, "rain", true, 4200), Entry(oct18.minusDays(20), "pink", false, 900))
        assertEquals(list.sortedBy { it.day }, ListeningLog.decode(ListeningLog.encode(list)))
        assertEquals("2026-10-18,rain,1,4200", ListeningLog.encode(listOf(Entry(oct18, "rain", true, 4200))))
    }

    @Test
    fun decodeSkipsDamagedEntries() {
        val text = "2026-10-18,rain,1,4200;oops;2026-13-40,pink,0,60;2026-10-18,,1,60;2026-10-18,pink,2,60;2026-10-18,pink,0,-5"
        assertEquals(listOf(Entry(oct18, "rain", true, 4200)), ListeningLog.decode(text))
        assertEquals(emptyList<Entry>(), ListeningLog.decode(null))
        assertEquals(emptyList<Entry>(), ListeningLog.decode(""))
    }

    @Test
    fun theDayChangesAtFourInTheMorning() {
        assertEquals(oct18, ListeningLog.sessionDay(LocalDateTime.of(2026, 10, 19, 3, 59)))
        assertEquals(oct18.plusDays(1), ListeningLog.sessionDay(LocalDateTime.of(2026, 10, 19, 4, 0)))
        assertEquals(oct18, ListeningLog.sessionDay(LocalDateTime.of(2026, 10, 18, 23, 30)))
        // Just after midnight on the 1st still belongs to the last day of the month before.
        assertEquals(LocalDate.of(2026, 9, 30), ListeningLog.sessionDay(LocalDateTime.of(2026, 10, 1, 0, 10)))
    }

    @Test
    fun addMergesTheSameSoundAndNotchAndKeepsTheRestApart() {
        var list = ListeningLog.add(emptyList(), Entry(oct18, "rain", true, 600))
        list = ListeningLog.add(list, Entry(oct18, "rain", true, 120))
        list = ListeningLog.add(list, Entry(oct18, "rain", false, 300))
        list = ListeningLog.add(list, Entry(oct18.minusDays(1), "rain", true, 60))
        list = ListeningLog.add(list, Entry(oct18, "pink", true, 0))   // nothing to add
        assertEquals(
            listOf(Entry(oct18.minusDays(1), "rain", true, 60), Entry(oct18, "rain", true, 720), Entry(oct18, "rain", false, 300)),
            list,
        )
    }

    @Test
    fun dayAndMonthTotals() {
        val list = listOf(
            Entry(oct18, "rain", true, 4200),
            Entry(oct18, "pink", false, 900),
            Entry(oct18.minusDays(1), "brown", false, 30),          // under a minute: not a listening day
            Entry(LocalDate.of(2026, 9, 30), "rain", true, 7200),   // other month
        )
        assertEquals(5100, ListeningLog.dayTotal(list, oct18))
        assertEquals(listOf(Entry(oct18, "rain", true, 4200), Entry(oct18, "pink", false, 900)), ListeningLog.forDay(list, oct18))
        assertEquals(ListeningLog.MonthTotals(days = 1, seconds = 5100), ListeningLog.monthTotals(list, oct))
        assertEquals(ListeningLog.MonthTotals(days = 1, seconds = 7200), ListeningLog.monthTotals(list, YearMonth.of(2026, 9)))
        assertEquals(ListeningLog.MonthTotals(0, 0), ListeningLog.monthTotals(emptyList(), oct))
    }

    @Test
    fun shadeSteps() {
        assertEquals(Shade.NONE, ListeningLog.shade(0))
        assertEquals(Shade.NONE, ListeningLog.shade(59))
        assertEquals(Shade.LIGHT, ListeningLog.shade(60))
        assertEquals(Shade.LIGHT, ListeningLog.shade(29 * 60 + 59))
        assertEquals(Shade.MID, ListeningLog.shade(30 * 60))
        assertEquals(Shade.MID, ListeningLog.shade(2 * 3600 - 1))
        assertEquals(Shade.FULL, ListeningLog.shade(2 * 3600))
    }

    @Test
    fun firstMonthIsTheEarliestRecordButNeverLaterThanNow() {
        assertEquals(oct, ListeningLog.firstMonth(emptyList(), emptyList(), oct))
        assertEquals(
            YearMonth.of(2026, 8),
            ListeningLog.firstMonth(listOf(Entry(LocalDate.of(2026, 9, 3), "rain", true, 60)), listOf(LocalDate.of(2026, 8, 30)), oct),
        )
    }

    @Test
    fun durationInKorean() {
        assertEquals("0분", ListeningLog.duration(59))
        assertEquals("35분", ListeningLog.duration(35 * 60 + 20))
        assertEquals("1시간 35분", ListeningLog.duration(95 * 60))
        assertEquals("2시간", ListeningLog.duration(2 * 3600 + 30))
    }
}
