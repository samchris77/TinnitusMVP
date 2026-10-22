package com.tinnitustracker.mvp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.BuildConfig
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Corners
import com.tinnitustracker.mvp.ui.theme.Palette

/** The only secondary screen: disclaimer, red flags, privacy policy, contact, version. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(onDismiss: () -> Unit, onFlags: () -> Unit) {
    val context = LocalContext.current
    // Privacy policy URL and support address do not exist yet; Play needs both before release.
    val notYet = { Toast.makeText(context, "데모: 아직 연결되지 않았어요", Toast.LENGTH_SHORT).show() }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("앱 정보", style = AppText.Title, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Palette.Ink2)
                }
            }
            Text(
                "이 앱은 의료기기가 아니에요. 이명을 진단하거나 치료하지 않아요. 이명이 갑자기 생기거나 달라지면 이비인후과 진료를 받으세요.",
                style = AppText.Body,
                color = Palette.Ink2,
            )
            Column(Modifier.fillMaxWidth().card()) {
                SheetRow("진료가 먼저 필요한 경우", onFlags)
                HorizontalDivider(color = Palette.Line)
                SheetRow("개인정보 처리방침", notYet)
                HorizontalDivider(color = Palette.Line)
                SheetRow("문의하기", notYet)
            }
            // CC0 needs no credit; it is given anyway. Sources: sounds/README.md.
            Text(
                "자연음 녹음: kvgarlic, Felix Blume, turbostream, Spandau (Freesound, CC0)",
                style = AppText.Caption,
                color = Palette.Ink2,
            )
            Text(
                "모든 정보는 이 휴대폰에만 저장돼요 · 버전 ${BuildConfig.VERSION_NAME}",
                style = AppText.Caption,
                color = Palette.Ink2,
            )
        }
    }
}

@Composable
private fun SheetRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AppText.Body, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
    }
}
