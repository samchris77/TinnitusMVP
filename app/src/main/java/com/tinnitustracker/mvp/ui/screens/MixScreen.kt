package com.tinnitustracker.mvp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tinnitustracker.mvp.ui.MixUi
import com.tinnitustracker.mvp.ui.components.Heading
import com.tinnitustracker.mvp.ui.components.HintChip
import com.tinnitustracker.mvp.ui.components.LabeledSlider
import com.tinnitustracker.mvp.ui.components.PrimaryButton
import com.tinnitustracker.mvp.ui.components.Readout
import com.tinnitustracker.mvp.ui.components.StepScaffold
import com.tinnitustracker.mvp.ui.components.Steppers
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Palette
import kotlin.math.roundToInt

/**
 * 내 볼륨 찾기: raise the media volume one step at a time until the tinnitus starts to blend
 * into the sound (the mixing point). Saving sets the volume one step below it.
 * The explanation line is a draft for clinical review.
 */
@Composable
fun MixScreen(
    mix: MixUi,
    volumeMax: Int,
    playing: Boolean,
    soundName: String,
    notchActive: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onLevel: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onSave: () -> Unit,
) {
    StepScaffold(
        title = "내 볼륨 찾기",
        onBack = onBack,
        footer = { PrimaryButton("여기서 섞이기 시작해요", onClick = onSave, enabled = mix.level > 0 && mix.heard) },
    ) {
        Heading("이명이 소리와 섞이는 지점을 찾아요", "작은 소리부터 한 칸씩 키우세요.\n이명이 소리에 섞여 흐려지기 시작하면 멈추세요.")
        HintChip(Icons.Filled.Headphones, "이어폰을 끼고 조용한 곳에서")
        Readout(
            value = "${mix.level} / $volumeMax",
            caption = if (notchActive) "$soundName · 이명 음 빼기 켜짐" else soundName,
            playing = playing,
            onToggle = onPlay,
        )
        LabeledSlider(
            value = mix.level.toFloat(),
            onValueChange = { onLevel(it.roundToInt()) },
            start = "작게",
            end = "크게",
            description = "볼륨",
            valueRange = 0f..volumeMax.toFloat(),
        )
        Steppers(minus = "한 칸 작게", plus = "한 칸 크게", onStep = onStep)
        Text(
            "이명이 완전히 가려지면 뇌가 이명에 익숙해지기 어려워요. 그래서 이 지점보다 한 칸 작게 들어요.",
            style = AppText.Caption,
            color = Palette.Ink2,
        )
    }
}
