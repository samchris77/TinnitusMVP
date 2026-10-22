package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.ui.components.AppMark
import com.tinnitustracker.mvp.ui.components.Badge
import com.tinnitustracker.mvp.ui.components.Heading
import com.tinnitustracker.mvp.ui.components.PrimaryButton
import com.tinnitustracker.mvp.ui.components.SafetyBox
import com.tinnitustracker.mvp.ui.components.SecondaryButton
import com.tinnitustracker.mvp.ui.components.StepScaffold
import com.tinnitustracker.mvp.ui.components.WarnNote
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Palette

// All patient-facing safety wording here is a draft for clinical review.

/** First launch only: what the app does, then the red-flag check. */
@Composable
fun WelcomeScreen(onAnswer: (anyApply: Boolean) -> Unit) {
    StepScaffold(
        topPadding = 24.dp,
        footer = {
            Text(
                "위에 해당하는 게 있나요?",
                style = AppText.Label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("아니요, 시작할게요", onClick = { onAnswer(false) })
            SecondaryButton("네, 해당돼요", onClick = { onAnswer(true) })
        },
    ) {
        AppMark()
        Heading("이명 사운드", "내 이명에 맞춘 소리를 들려드려요.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberedLine(1, "내 이명의 높이와 크기를 찾아요")
            NumberedLine(2, "노이즈나 자연음을 들어요. 내 이명 음만 빼고 들을 수도 있어요.")
        }
        SafetyBox()
        Text("이 앱은 의료기기가 아니며, 이명을 진단하거나 치료하지 않아요.", style = AppText.Caption, color = Palette.Ink2)
    }
}

@Composable
private fun NumberedLine(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .background(Palette.TealSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = AppText.Caption.copy(fontWeight = FontWeight.Bold), color = Palette.Teal)
        }
        Text(text, style = AppText.Body)
    }
}

/** Shown when any red flag applies. The wording is never softened. */
@Composable
fun ReferralScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    StepScaffold(
        onBack = onBack,
        centered = true,
        footer = { SecondaryButton("이미 진료를 받았어요", onClick = onContinue) },
    ) {
        Badge(Icons.Filled.LocalHospital, tint = Palette.Warn, background = Palette.WarnSoft)
        Heading("먼저 이비인후과 진료를\n받아보세요", "이런 증상은 다른 원인이 있는지 먼저 확인해야 해요.", center = true)
        WarnNote(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("갑자기 잘 안 들리게 됐다면 ") }
                append("바로 이비인후과나 응급실에 가세요. 며칠 안에 치료를 시작해야 청력을 지킬 수 있어요.")
            },
        )
    }
}

/** The red-flag list again, from 앱 정보. */
@Composable
fun FlagsScreen(onBack: () -> Unit) {
    StepScaffold(title = "진료가 먼저 필요한 경우", onBack = onBack) {
        SafetyBox()
        Text(
            "하나라도 해당하면 이비인후과 진료를 받으세요. 갑자기 잘 안 들리게 됐다면 바로 가세요.",
            style = AppText.Body,
            color = Palette.Ink2,
        )
    }
}
