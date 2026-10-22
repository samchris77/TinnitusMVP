package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.ui.Finder
import com.tinnitustracker.mvp.ui.components.Badge
import com.tinnitustracker.mvp.ui.components.ChoiceRow
import com.tinnitustracker.mvp.ui.components.Heading
import com.tinnitustracker.mvp.ui.components.HintChip
import com.tinnitustracker.mvp.ui.components.LabeledSlider
import com.tinnitustracker.mvp.ui.components.PrimaryButton
import com.tinnitustracker.mvp.ui.components.Readout
import com.tinnitustracker.mvp.ui.components.StepScaffold
import com.tinnitustracker.mvp.ui.components.Steppers
import com.tinnitustracker.mvp.ui.components.TextAction
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Palette
import com.tinnitustracker.mvp.util.Pitch
import kotlin.math.roundToInt

private const val FINDER_TITLE = "내 이명 찾기"

/** Step 1: slide until the tone matches the tinnitus. */
@Composable
fun PitchScreen(
    finder: Finder,
    onBack: () -> Unit,
    onToggleTone: () -> Unit,
    onPitch: (Float) -> Unit,
    onStep: (Int) -> Unit,
    onNoClearTone: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(
        title = FINDER_TITLE,
        step = "1 / 3",
        onBack = onBack,
        footer = {
            TextAction("뚜렷한 음이 아니에요", onClick = onNoClearTone)
            PrimaryButton("다음", onClick = onNext)
        },
    ) {
        Heading("이명과 같은 높이를 찾아요", "막대를 움직여 이명과 가장 비슷한 높이에 맞추세요.")
        HintChip(Icons.Filled.Headphones, "이어폰을 끼고 조용한 곳에서")
        Readout(
            value = Pitch.format(finder.hz),
            caption = Pitch.describe(finder.hz),
            playing = finder.toneOn,
            onToggle = onToggleTone,
        )
        LabeledSlider(
            value = Pitch.toFraction(finder.hz),
            onValueChange = onPitch,
            start = "낮은 음",
            end = "높은 음",
            description = "이명 높이",
        )
        Steppers(minus = "조금 낮게", plus = "조금 높게", onStep = onStep)
    }
}

/** Step 2: rule out an octave mix-up. */
@Composable
fun OctaveScreen(finder: Finder, onBack: () -> Unit, onTry: (Int) -> Unit, onNext: () -> Unit) {
    val names = mapOf(-1 to "더 낮은 음", 0 to "찾은 음", 1 to "더 높은 음")
    StepScaffold(
        title = FINDER_TITLE,
        step = "2 / 3",
        onBack = onBack,
        footer = { PrimaryButton("다음", onClick = onNext) },
    ) {
        Heading("한 번 더 확인해요", "한 옥타브 높거나 낮은 음과 헷갈리기 쉬워요. 하나씩 눌러 듣고 가장 비슷한 것을 고르세요.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            finder.octaveOptions.forEach { (octave, hz) ->
                ChoiceRow(
                    title = names.getValue(octave),
                    detail = Pitch.format(hz),
                    selected = finder.octave == octave,
                    playing = finder.playingOctave == octave,
                    onClick = { onTry(octave) },
                )
            }
        }
    }
}

/** Step 3: raise the sound from silence until it is as loud as the tinnitus. */
@Composable
fun LoudnessScreen(
    finder: Finder,
    onBack: () -> Unit,
    onToggleTone: () -> Unit,
    onLoudness: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onDone: () -> Unit,
) {
    StepScaffold(
        title = FINDER_TITLE,
        step = "3 / 3",
        onBack = onBack,
        footer = { PrimaryButton("완료", onClick = onDone, enabled = finder.loudness > 0) },
    ) {
        Heading("이명과 같은 크기로 맞춰요", "작은 소리부터 시작해요.\n이명과 크기가 같아지면 멈추세요.")
        Readout(
            value = "${finder.loudness}",
            caption = if (finder.nonTonal) "쉬— 하는 소리" else "${Pitch.format(finder.hz)} 소리",
            playing = finder.toneOn,
            onToggle = onToggleTone,
        )
        LabeledSlider(
            value = finder.loudness.toFloat(),
            onValueChange = { onLoudness(it.roundToInt()) },
            start = "작게",
            end = "크게",
            description = "이명 크기",
            valueRange = 0f..100f,
        )
        Steppers(minus = "조금 작게", plus = "조금 크게", onStep = onStep)
    }
}

@Composable
fun ResultScreen(finder: Finder, onBack: () -> Unit, onRedo: () -> Unit, onSave: () -> Unit) {
    StepScaffold(
        title = FINDER_TITLE,
        onBack = onBack,
        centered = true,
        footer = {
            TextAction("다시 하기", onClick = onRedo)
            PrimaryButton("저장하고 소리 듣기", onClick = onSave)
        },
    ) {
        Badge(Icons.Filled.Check, tint = Palette.Teal, background = Palette.TealSoft)
        Text("내 이명을 찾았어요", style = AppText.Title)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(
                label = "높이",
                value = if (finder.nonTonal) "—" else Pitch.format(finder.hz),
                note = if (finder.nonTonal) "뚜렷한 음 없음" else Pitch.describe(finder.hz),
                modifier = Modifier.weight(1f),
            )
            Stat(label = "크기", value = "${finder.loudness}", note = "0–100 기준", modifier = Modifier.weight(1f))
        }
        Text(
            "크기는 지금 쓰는 이어폰과 휴대폰 볼륨 기준이에요.\n다음에도 같은 조건에서 재야 비교할 수 있어요.",
            style = AppText.Caption,
            color = Palette.Ink2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = AppText.Caption, color = Palette.Ink2)
        Text(value, style = AppText.Title)
        Text(note, style = AppText.Caption, color = Palette.Ink2)
    }
}
