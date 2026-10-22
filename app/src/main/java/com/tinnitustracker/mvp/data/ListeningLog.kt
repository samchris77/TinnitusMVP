package com.tinnitustracker.mvp.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * The listening record behind the 내 기록 calendar: seconds played per day, sound and notch
 * setting. It fills itself while sound plays and is never edited.
 */
object ListeningLog {
    data class Entry(val day: LocalDate, val soundId: String, val notch: Boolean, val seconds: Int)

    data class MonthTotals(val days: Int, val seconds: Int)

    /** Calendar shading: nothing, under 30 minutes, 30 minutes to 2 hours, 2 hours or more. */
    enum class Shade { NONE, LIGHT, MID, FULL }

    /** A session shorter than this is dropped, so an accidental tap never shows up. */
    const val MIN_SESSION_SECONDS = 60

    /** A night of listening stays on one day: the day changes at 4 a.m., not midnight. */
    private val DAY_STARTS = LocalTime.of(4, 0)

    /** "2026-10-18,rain,1,4200;2026-10-18,pink,0,900" — small enough for one DataStore string. */
    fun encode(list: List<Entry>): String =
        list.joinToString(";") { "${it.day},${it.soundId},${if (it.notch) 1 else 0},${it.seconds}" }

    fun decode(text: String?): List<Entry> =
        text.orEmpty().split(';').mapNotNull { entry ->
            val f = entry.split(',')
            if (f.size != 4 || f[1].isEmpty()) return@mapNotNull null
            val notch = when (f[2]) {
                "1" -> true
                "0" -> false
                else -> return@mapNotNull null
            }
            val seconds = f[3].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            runCatching { Entry(LocalDate.parse(f[0]), f[1], notch, seconds) }.getOrNull()
        }.sortedBy { it.day }

    /** The day a session started at [start] counts toward. Before 4 a.m. that is the day before. */
    fun sessionDay(start: LocalDateTime): LocalDate =
        if (start.toLocalTime() < DAY_STARTS) start.toLocalDate().minusDays(1) else start.toLocalDate()

    /** Adds [entry]'s time to the entry with the same day, sound and notch setting, or appends it. */
    fun add(list: List<Entry>, entry: Entry): List<Entry> {
        if (entry.seconds <= 0) return list
        val i = list.indexOfFirst { it.day == entry.day && it.soundId == entry.soundId && it.notch == entry.notch }
        if (i < 0) return (list + entry).sortedBy { it.day }
        return list.toMutableList().also { it[i] = it[i].copy(seconds = it[i].seconds + entry.seconds) }
    }

    /** What was played on [day], longest first. */
    fun forDay(list: List<Entry>, day: LocalDate): List<Entry> =
        list.filter { it.day == day }.sortedByDescending { it.seconds }

    fun dayTotal(list: List<Entry>, day: LocalDate): Int = list.filter { it.day == day }.sumOf { it.seconds }

    /** Seconds per day, for shading a month at once. */
    fun dayTotals(list: List<Entry>): Map<LocalDate, Int> =
        list.groupingBy { it.day }.fold(0) { sum, e -> sum + e.seconds }

    /** Days with some listening in [month], and their total time. */
    fun monthTotals(list: List<Entry>, month: YearMonth): MonthTotals {
        val days = dayTotals(list).filter { (day, seconds) -> YearMonth.from(day) == month && shade(seconds) != Shade.NONE }
        return MonthTotals(days.size, days.values.sum())
    }

    fun shade(seconds: Int): Shade = when {
        seconds < MIN_SESSION_SECONDS -> Shade.NONE
        seconds < 30 * 60 -> Shade.LIGHT
        seconds < 2 * 60 * 60 -> Shade.MID
        else -> Shade.FULL
    }

    /** The earliest month with listening or a check-in, so ‹ stops there; never later than [now]. */
    fun firstMonth(list: List<Entry>, checkInDays: List<LocalDate>, now: YearMonth): YearMonth =
        (list.map { YearMonth.from(it.day) } + checkInDays.map { YearMonth.from(it) } + now).min()

    /** "35분", "1시간 35분", "2시간". Seconds are dropped. */
    fun duration(seconds: Int): String {
        val minutes = seconds / 60
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }
}
