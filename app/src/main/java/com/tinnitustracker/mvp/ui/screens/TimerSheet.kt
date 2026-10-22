package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.ui.TIMER_CHOICES
import com.tinnitustracker.mvp.ui.components.RadioDot
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Corners
import com.tinnitustracker.mvp.ui.theme.Palette

/** One tap picks a duration; choosing closes the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSheet(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
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
                Text("수면 타이머", style = AppText.Title, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Palette.Ink2)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .card()
                    .selectableGroup(),
            ) {
                TIMER_CHOICES.forEachIndexed { i, (minutes, label) ->
                    if (i > 0) HorizontalDivider(color = Palette.Line)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = minutes == selected, role = Role.RadioButton, onClick = { onSelect(minutes) })
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = AppText.Body, modifier = Modifier.weight(1f))
                        RadioDot(minutes == selected)
                    }
                }
            }
            Text(
                "끝나기 30초 전부터 소리가 천천히 작아져요. 일시정지하면 타이머도 멈춰요.",
                style = AppText.Caption,
                color = Palette.Ink2,
            )
        }
    }
}
