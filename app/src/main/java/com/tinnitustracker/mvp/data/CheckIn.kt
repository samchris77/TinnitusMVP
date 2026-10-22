package com.tinnitustracker.mvp.data

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * One weekly K-TMI check-in (Hwang & Bahng 2018, validated Korean version):
 * Q1 loudness 0–10, Q2 awareness 0–100 % in 10 % steps, Q3 severity 0–100 on a visual analogue scale.
 */
data class CheckIn(val date: LocalDate, val q1: Int, val q2: Int, val q3: Int) {

    /** The validated weighted composite, 0–100. Lower means the tinnitus was noticed less. */
    val score: Int get() = (q1 * 10 * 0.5856 + q2 * 0.0423 + q3 * 0.3721).roundToInt()

    companion object {
        const val INTERVAL_DAYS = 7L

        /** "2026-10-21,5,70,42;2026-10-28,4,60,38" — small enough for one DataStore string. */
        fun encode(list: List<CheckIn>): String =
            list.joinToString(";") { "${it.date},${it.q1},${it.q2},${it.q3}" }

        fun decode(text: String?): List<CheckIn> =
            text.orEmpty().split(';').mapNotNull { entry ->
                val f = entry.split(',')
                if (f.size != 4) return@mapNotNull null
                runCatching { CheckIn(LocalDate.parse(f[0]), f[1].toInt(), f[2].toInt(), f[3].toInt()) }.getOrNull()
            }.sortedBy { it.date }

        /** Adds [entry], replacing any check-in from the same day. */
        fun add(list: List<CheckIn>, entry: CheckIn): List<CheckIn> =
            (list.filter { it.date != entry.date } + entry).sortedBy { it.date }

        fun nextDue(list: List<CheckIn>, today: LocalDate): LocalDate =
            list.lastOrNull()?.date?.plusDays(INTERVAL_DAYS) ?: today

        fun isDue(list: List<CheckIn>, today: LocalDate): Boolean = !today.isBefore(nextDue(list, today))
    }
}
