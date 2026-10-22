package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.data.CheckIn
import com.tinnitustracker.mvp.data.ListeningLog
import com.tinnitustracker.mvp.data.ListeningLog.Shade
import com.tinnitustracker.mvp.data.Sound
import com.tinnitustracker.mvp.ui.components.MessageToast
import com.tinnitustracker.mvp.ui.components.StepScaffold
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Corners
import com.tinnitustracker.mvp.ui.theme.Palette
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 내 기록: a month of listening time on top, then the weekly score, chart and history.
 * The calendar only records what played; it sets no goal and keeps no streak.
 */
@Composable
fun RecordsScreen(
    checkIns: List<CheckIn>,
    listening: List<ListeningLog.Entry>,
    message: String?,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
) {
    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var openDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    Box(Modifier.fillMaxSize()) {
        StepScaffold(title = "내 기록", onBack = onBack) {
            MonthCard(
                month = month,
                today = today,
                listening = listening,
                checkIns = checkIns,
                onMonth = { month = month.plusMonths(it.toLong()) },
                onDay = { openDay = it },
            )
            ScoreSection(checkIns, onCheckIn)
        }
        MessageToast(
            message,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        )
    }
    openDay?.let { day ->
        DaySheet(
            day = day,
            today = today,
            listening = listening,
            checkIn = checkIns.firstOrNull { it.date == day },
            onDismiss = { openDay = null },
        )
    }
}

// ── Month ────────────────────────────────────────────────────────────────────

@Composable
private fun MonthCard(
    month: YearMonth,
    today: LocalDate,
    listening: List<ListeningLog.Entry>,
    checkIns: List<CheckIn>,
    onMonth: (Int) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val current = YearMonth.from(today)
    val first = remember(listening, checkIns, current) { ListeningLog.firstMonth(listening, checkIns.map { it.date }, current) }
    val dayTotals = remember(listening) { ListeningLog.dayTotals(listening) }
    val checkInDays = remember(checkIns) { checkIns.map { it.date }.toSet() }
    val totals = remember(listening, month) { ListeningLog.monthTotals(listening, month) }
    val summary = when {
        totals.days > 0 -> "${totals.days}일 들었어요 · 총 ${ListeningLog.duration(totals.seconds)}"
        listening.isEmpty() -> "소리를 틀면 들은 시간이 여기에 쌓여요"
        month == current -> "이번 달은 아직 듣지 않았어요"
        else -> "이 달은 듣지 않았어요"
    }
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val lead = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val length = month.lengthOfMonth()

    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${month.year}년 ${month.monthValue}월", style = AppText.BodyStrong)
                Text(summary, style = AppText.Caption, color = Palette.Ink2)
            }
            MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 달", enabled = month > first) { onMonth(-1) }
            MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 달", enabled = month < current) { onMonth(1) }
        }
        Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) {
                    Text(
                        firstDayOfWeek.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.KOREAN),
                        style = AppText.Caption,
                        color = Palette.Ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                    )
                }
            }
            val rows = (lead + length + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val d = row * 7 + col - lead + 1
                        if (d < 1 || d > length) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val date = month.atDay(d)
                            val future = date.isAfter(today)
                            DayCell(
                                date = date,
                                seconds = if (future) 0 else dayTotals[date] ?: 0,
                                today = date == today,
                                future = future,
                                checkIn = date in checkInDays,
                                onClick = { onDay(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        Legend()
    }
}

@Composable
private fun MonthButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = label, tint = if (enabled) Palette.Ink2 else Palette.Ink2.copy(alpha = 0.3f))
    }
}

/** One day: shaded by listening time, outlined when it is today, a dot when there was a check-in. */
@Composable
private fun DayCell(
    date: LocalDate,
    seconds: Int,
    today: Boolean,
    future: Boolean,
    checkIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shade = ListeningLog.shade(seconds)
    val fill = when (shade) {
        Shade.NONE -> Color.Transparent
        Shade.LIGHT -> Palette.Shade1
        Shade.MID -> Palette.Shade2
        Shade.FULL -> Palette.Shade3
    }
    val ink = if (shade == Shade.FULL) Palette.Surface else Palette.Ink
    val label = buildString {
        append("${date.monthValue}월 ${date.dayOfMonth}일")
        if (!future) {
            if (today) append(" 오늘")
            append(if (shade == Shade.NONE) ", 안 들음" else ", ${ListeningLog.duration(seconds)} 들음")
            if (checkIn) append(", 체크인")
        }
    }
    Box(
        modifier
            .aspectRatio(1f)
            .then(if (future) Modifier.alpha(0.55f) else Modifier)
            .drawBehind {
                if (today) {
                    // A 2 dp ring just outside the cell, like the design's outline with a 1 px offset.
                    val gap = 3.dp.toPx()
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = Palette.Ink,
                        topLeft = Offset(-gap + stroke / 2, -gap + stroke / 2),
                        size = Size(size.width + 2 * gap - stroke, size.height + 2 * gap - stroke),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(stroke),
                    )
                }
            }
            .clip(Corners.Chip)
            .background(fill)
            .then(
                if (future) {
                    Modifier.semantics {
                        contentDescription = label
                        disabled()
                    }
                } else {
                    Modifier
                        .clickable(role = Role.Button, onClick = onClick)
                        .semantics { contentDescription = label }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = AppText.Label.copy(fontWeight = if (today) FontWeight.Bold else FontWeight.Medium, fontFeatureSettings = "tnum"),
            color = if (future) Palette.Muted else ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
        if (checkIn) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .background(if (shade == Shade.FULL) Palette.Surface else Palette.Ink, CircleShape),
            )
        }
    }
}

/** Names the shade steps, so colour is never the only cue. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendItem("30분 미만") { Swatch(Palette.Shade1) }
        LegendItem("30분–2시간") { Swatch(Palette.Shade2) }
        LegendItem("2시간 이상") { Swatch(Palette.Shade3) }
        LegendItem("체크인") {
            Box(Modifier.padding(horizontal = 3.dp).size(4.dp).background(Palette.Ink, CircleShape))
        }
    }
}

@Composable
private fun LegendItem(text: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        mark()
        Text(text, style = AppText.Caption, color = Palette.Ink2)
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
}

// ── Day ──────────────────────────────────────────────────────────────────────

/** What played that day, longest first, and the check-in if there was one. Read-only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    day: LocalDate,
    today: LocalDate,
    listening: List<ListeningLog.Entry>,
    checkIn: CheckIn?,
    onDismiss: () -> Unit,
) {
    val entries = ListeningLog.forDay(listening, day).filter { it.seconds >= ListeningLog.MIN_SESSION_SECONDS }
    val total = ListeningLog.dayTotal(listening, day)
    val isToday = day == today
    val headline = when {
        ListeningLog.shade(total) != Shade.NONE -> "${if (isToday) "지금까지 " else ""}${ListeningLog.duration(total)} 들었어요"
        isToday -> "오늘은 아직 듣지 않았어요"
        else -> "이날은 소리를 듣지 않았어요"
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Bg,
        shape = Corners.Panel,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(Palette.Ink.copy(alpha = 0.2f), RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    Text(longDate(day), style = AppText.Title)
                    Text(headline, style = AppText.Body, color = Palette.Ink2)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Palette.Ink2)
                }
            }
            if (entries.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().card()) {
                    entries.forEachIndexed { i, entry ->
                        if (i > 0) HorizontalDivider(color = Palette.Line)
                        EntryRow(entry)
                    }
                }
            }
            if (checkIn != null) {
                SheetRow(
                    visual = { Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = Palette.Teal) },
                    title = "체크인",
                    detail = "크기 ${checkIn.q1} · 인지 ${checkIn.q2}% · 심각도 ${checkIn.q3}",
                    value = "${checkIn.score}점",
                    modifier = Modifier.card(),
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: ListeningLog.Entry) {
    val sound = Sound.entries.firstOrNull { it.id == entry.soundId }
    val icon = sound?.icon()
    SheetRow(
        visual = {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Palette.Ink2)
            } else {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(Palette.Swatches[entry.soundId] ?: Palette.Track, CircleShape)
                        .border(1.dp, Palette.Ink.copy(alpha = 0.14f), CircleShape),
                )
            }
        },
        title = sound?.fullName ?: entry.soundId,
        detail = "이명 음 빼기 ${if (entry.notch) "켜짐" else "꺼짐"}",
        value = ListeningLog.duration(entry.seconds),
    )
}

@Composable
private fun SheetRow(
    visual: @Composable () -> Unit,
    title: String,
    detail: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { visual() }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.Body)
            Text(detail, style = AppText.Caption, color = Palette.Ink2)
        }
        Text(value, style = AppText.Label.copy(fontFeatureSettings = "tnum"))
    }
}
