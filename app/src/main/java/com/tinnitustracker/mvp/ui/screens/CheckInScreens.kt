package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.data.CheckIn
import com.tinnitustracker.mvp.ui.CheckInUi
import com.tinnitustracker.mvp.ui.components.LabeledSlider
import com.tinnitustracker.mvp.ui.components.PrimaryButton
import com.tinnitustracker.mvp.ui.components.Readout
import com.tinnitustracker.mvp.ui.components.StepScaffold
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Palette
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** One K-TMI question. The wording and anchors are the published validated text; do not edit. */
private class Question(
    val title: String,
    val min: Int,
    val max: Int,
    val step: Int,
    val low: String,
    val high: String,
    val unit: String,
    val range: String,
    val showsNumber: Boolean,
)

private val QUESTIONS = listOf(
    Question("지난 한 주 동안, 이명이 얼마나 크게 들렸습니까?", 0, 10, 1, "전혀 크지 않았다", "매우 크게 들렸다", "", "0–10", true),
    Question("깨어 있는 시간 중에 이명을 인지하는 시간은 몇 % 정도 되었습니까?", 0, 100, 10, "전혀 인지하지 못하였다", "항상 인지하였다", "%", "0–100%", true),
    // Q3 is a visual analogue scale: no number on screen, no fill along the track.
    Question("지난 한 주 동안, 이명이 얼마나 심각하게 느껴졌습니까?", 0, 100, 1, "전혀 심각하지 않았다", "매우 심각하다고 느꼈다", "", "", false),
)

@Composable
fun CheckInScreen(ui: CheckInUi, onBack: () -> Unit, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    val q = QUESTIONS[ui.step]
    val value = ui.current
    StepScaffold(
        title = "이번 주 체크인",
        step = "${ui.step + 1} / 3",
        onBack = onBack,
        footer = { PrimaryButton(if (ui.step < 2) "다음" else "완료", onClick = onNext, enabled = value != null) },
    ) {
        Text(q.title, style = AppText.Title)
        if (q.showsNumber) {
            Readout(value = value?.let { "$it${q.unit}" } ?: "—", caption = q.range)
        } else {
            Text("막대 위, 느낌에 맞는 곳을 누르세요.", style = AppText.Body, color = Palette.Ink2)
        }
        // Nothing is pre-selected: the thumb stays hidden until the first touch.
        LabeledSlider(
            value = (value ?: (q.min + q.max) / 2).toFloat(),
            onValueChange = { onAnswer(((it / q.step).roundToInt() * q.step).coerceIn(q.min, q.max)) },
            start = q.low,
            end = q.high,
            description = q.title,
            valueRange = q.min.toFloat()..q.max.toFloat(),
            showThumb = value != null,
            filled = q.showsNumber && value != null,
            trackHeight = if (q.showsNumber) 6.dp else 8.dp,
        )
    }
}

/**
 * The lower half of 내 기록, as v1.1's 내 변화 had it: this week's score, the trend and every
 * check-in. It adds its cards straight into the caller's column.
 */
@Composable
internal fun ScoreSection(checkIns: List<CheckIn>, onCheckIn: () -> Unit) {
    if (checkIns.isEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("아직 체크인이 없어요", style = AppText.BodyStrong)
            Text("일주일에 한 번, 질문 3개로 이명이 어떻게 달라지는지 기록해요.", style = AppText.Caption, color = Palette.Ink2)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("첫 체크인 · 2분", onClick = onCheckIn)
        }
        return
    }
    val today = LocalDate.now()
    val due = CheckIn.isDue(checkIns, today)
    ScoreCard(checkIns, due)
    if (checkIns.size >= 2) {
        ChartCard(checkIns)
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Bg, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("두 번 이상 기록하면 변화가 그래프로 보여요", style = AppText.BodyStrong)
            }
        }
    }
    if (due) {
        PrimaryButton("이번 주 체크인 · 2분", onClick = onCheckIn)
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .card()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("다음 체크인 · ${longDate(CheckIn.nextDue(checkIns, today))}", style = AppText.Body)
        }
    }
    Text("체크인 기록", style = AppText.Label, color = Palette.Ink2, modifier = Modifier.padding(start = 4.dp))
    History(checkIns)
    Text("지난 한 주에 대한 내 느낌을 적은 기록이에요. 진단이 아니에요.", style = AppText.Caption, color = Palette.Ink2)
}

@Composable
private fun ScoreCard(checkIns: List<CheckIn>, due: Boolean) {
    val last = checkIns.last()
    val first = checkIns.first()
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (due) "최근 이명 점수" else "이번 주 이명 점수", style = AppText.Caption, color = Palette.Ink2)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${last.score}", style = AppText.Display)
            Text(" 점", style = AppText.BodyStrong, color = Palette.Ink2, modifier = Modifier.padding(bottom = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (checkIns.size >= 2 && last.score != first.score) {
                Icon(
                    if (last.score < first.score) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = Palette.Ink2,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                scoreChange(last.score, if (checkIns.size >= 2) first.score else null, than = "첫 기록보다", same = "첫 기록과 같아요"),
                style = AppText.Label,
                color = Palette.Ink2,
            )
        }
        Text("0–100점 · 낮을수록 이명이 덜 느껴졌다는 뜻이에요", style = AppText.Caption, color = Palette.Ink2)
    }
}

@Composable
private fun ChartCard(checkIns: List<CheckIn>) {
    val shown = checkIns.takeLast(MAX_POINTS)
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text("이명 점수", style = AppText.Label)
            Text(
                if (shown.size < checkIns.size) "최근 ${MAX_POINTS}번 · 질문 3개 (K-TMI)" else "주 1회 · 질문 3개 (K-TMI)",
                style = AppText.Caption,
                color = Palette.Ink2,
            )
        }
        ScoreChart(shown)
    }
}

private const val MAX_POINTS = 12

/**
 * One series, so no legend: a 2 dp line, ringed markers, a 10 % wash, hairline gridlines and a
 * label on the last point only. Tapping or dragging shows a crosshair with the date and score;
 * the history list below is the table view.
 */
@Composable
private fun ScoreChart(list: List<CheckIn>) {
    val measurer = rememberTextMeasurer()
    var selected by remember(list) { mutableStateOf<Int?>(null) }
    val axisStyle = AppText.Caption.copy(color = Palette.Ink2)
    val endStyle = AppText.Caption.copy(color = Palette.Ink, fontWeight = FontWeight.SemiBold)
    val tipValueStyle = AppText.Label.copy(color = Palette.Surface)
    val tipDateStyle = AppText.Caption.copy(color = Palette.Surface.copy(alpha = 0.8f))
    val n = list.size
    val summary = list.joinToString(", ") { "${it.date.monthValue}월 ${it.date.dayOfMonth}일 ${it.score}점" }

    fun plotX(i: Int, width: Float, left: Float, right: Float): Float =
        if (n == 1) left + (width - left - right) / 2 else left + i * (width - left - right) / (n - 1)

    fun nearest(x: Float, width: Float, left: Float, right: Float): Int =
        if (n == 1) 0 else ((x - left) / ((width - left - right) / (n - 1))).roundToInt().coerceIn(0, n - 1)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .semantics { contentDescription = "주별 이명 점수: $summary" }
            .pointerInput(list) {
                detectTapGestures { selected = nearest(it.x, size.width.toFloat(), 30.dp.toPx(), 34.dp.toPx()) }
            }
            .pointerInput(list) {
                detectHorizontalDragGestures { change, _ ->
                    selected = nearest(change.position.x, size.width.toFloat(), 30.dp.toPx(), 34.dp.toPx())
                }
            },
    ) {
        val left = 30.dp.toPx()
        val right = 34.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 28.dp.toPx()
        val plotHeight = size.height - top - bottom
        fun y(score: Int) = top + (1 - score / 100f) * plotHeight

        listOf(0, 50, 100).forEach { v ->
            drawLine(Palette.Line, Offset(left, y(v)), Offset(size.width - right + 8.dp.toPx(), y(v)), strokeWidth = 1.dp.toPx())
            val label = measurer.measure("$v", axisStyle)
            drawText(label, topLeft = Offset(left - 8.dp.toPx() - label.size.width, y(v) - label.size.height / 2f))
        }

        val points = list.mapIndexed { i, c -> Offset(plotX(i, size.width, left, right), y(c.score)) }
        val area = Path().apply {
            moveTo(points.first().x, y(0))
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, y(0))
            close()
        }
        drawPath(area, Palette.Teal.copy(alpha = 0.10f))

        selected?.let { i -> drawLine(Palette.Ink2, Offset(points[i].x, top), Offset(points[i].x, top + plotHeight), strokeWidth = 1.dp.toPx()) }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, Palette.Teal, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEach {
            drawCircle(Palette.Surface, radius = 6.dp.toPx(), center = it)   // 2 dp ring
            drawCircle(Palette.Teal, radius = 4.dp.toPx(), center = it)
        }

        val end = measurer.measure("${list.last().score}", endStyle)
        drawText(end, topLeft = Offset(points.last().x + 10.dp.toPx(), points.last().y - end.size.height / 2f))

        val every = max(1, (n + 5) / 6)   // at most about six date labels
        list.forEachIndexed { i, c ->
            if (i % every == 0 || i == n - 1) {
                val label = measurer.measure("${c.date.monthValue}/${c.date.dayOfMonth}", axisStyle)
                drawText(label, topLeft = Offset(points[i].x - label.size.width / 2f, size.height - label.size.height))
            }
        }

        selected?.let { i ->
            val value = measurer.measure("${list[i].score}점", tipValueStyle)
            val date = measurer.measure("${list[i].date.monthValue}월 ${list[i].date.dayOfMonth}일", tipDateStyle)
            val pad = 10.dp.toPx()
            val w = max(value.size.width, date.size.width) + pad * 2
            val h = value.size.height + date.size.height + 12.dp.toPx()
            val x = (points[i].x - w / 2).coerceIn(0f, size.width - w)
            val yTop = (points[i].y - h - 10.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(Palette.Ink, Offset(x, yTop), Size(w, h), CornerRadius(8.dp.toPx()))
            drawText(value, topLeft = Offset(x + pad, yTop + 6.dp.toPx()))
            drawText(date, topLeft = Offset(x + pad, yTop + 6.dp.toPx() + value.size.height))
        }
    }
}

@Composable
private fun History(checkIns: List<CheckIn>) {
    Column(Modifier.fillMaxWidth().card()) {
        checkIns.reversed().forEachIndexed { i, c ->
            if (i > 0) HorizontalDivider(color = Palette.Line)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${c.date.monthValue}월 ${c.date.dayOfMonth}일", style = AppText.BodyStrong)
                    Text("크기 ${c.q1} · 인지 ${c.q2}% · 심각도 ${c.q3}", style = AppText.Caption, color = Palette.Ink2)
                }
                Text("${c.score}점", style = AppText.BodyStrong)
            }
        }
    }
}

/** "10월 28일 수요일". */
internal fun longDate(d: LocalDate): String =
    "${d.monthValue}월 ${d.dayOfMonth}일 ${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"
