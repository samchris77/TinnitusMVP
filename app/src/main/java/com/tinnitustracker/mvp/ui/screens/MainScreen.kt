package com.tinnitustracker.mvp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.audio.PlaybackController
import com.tinnitustracker.mvp.data.CheckIn
import com.tinnitustracker.mvp.data.Profile
import com.tinnitustracker.mvp.data.Sound
import com.tinnitustracker.mvp.ui.components.AppBar
import com.tinnitustracker.mvp.ui.components.AppSlider
import com.tinnitustracker.mvp.ui.components.InlineAction
import com.tinnitustracker.mvp.ui.components.MessageToast
import com.tinnitustracker.mvp.ui.components.PlayButton
import com.tinnitustracker.mvp.ui.components.SmallPrimaryButton
import com.tinnitustracker.mvp.ui.components.SoundTile
import com.tinnitustracker.mvp.ui.components.SwitchRow
import com.tinnitustracker.mvp.ui.components.card
import com.tinnitustracker.mvp.ui.components.tappable
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Corners
import com.tinnitustracker.mvp.ui.theme.Palette
import com.tinnitustracker.mvp.util.Pitch
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The app's one main screen: my tinnitus, pick a sound, notch on or off, volume and play. */
@Composable
fun MainScreen(
    profile: Profile?,
    checkIns: List<CheckIn>,
    mixingPoint: Int?,
    playback: PlaybackController.State,
    timer: PlaybackController.Timer,
    volume: Int,
    volumeMax: Int,
    message: String?,
    onFind: () -> Unit,
    onCheckIn: () -> Unit,
    onRecords: () -> Unit,
    onInfo: () -> Unit,
    onSound: (Sound) -> Unit,
    onNotch: () -> Unit,
    onPlay: () -> Unit,
    onVolume: (Int) -> Unit,
    onMix: () -> Unit,
    onTimer: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .statusBarsPadding(),
            ) {
                AppBar(title = "이명 사운드", large = true, onRecords = onRecords, onInfo = onInfo)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    MyTinnitus(profile, checkIns, onFind, onCheckIn, onRecords)
                    SoundPicker(selected = playback.sound, onSound = onSound)
                    val hz = profile?.hz
                    SwitchRow(
                        title = "이명 음 빼기",
                        subtitle = when {
                            hz != null -> "${Pitch.format(hz)} 주변을 비우고 들려줘요"
                            profile != null -> "뚜렷한 음이 없으면 쓸 수 없어요"
                            else -> "내 이명을 찾으면 쓸 수 있어요"
                        },
                        checked = playback.notchActive,
                        enabled = hz != null,
                        onToggle = onNotch,
                    )
                }
            }
            Player(
                playing = playback.playing,
                timer = timer,
                volume = volume,
                volumeMax = volumeMax,
                mixingPoint = mixingPoint,
                onPlay = onPlay,
                onVolume = onVolume,
                onMix = onMix,
                onTimer = onTimer,
            )
        }
        MessageToast(
            message,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 220.dp),
        )
    }
}

@Composable
private fun MyTinnitus(
    profile: Profile?,
    checkIns: List<CheckIn>,
    onFind: () -> Unit,
    onCheckIn: () -> Unit,
    onRecords: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().card()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("내 이명", style = AppText.Caption, color = Palette.Ink2)
                if (profile == null) {
                    Text("아직 찾지 않았어요", style = AppText.Body, color = Palette.Ink2)
                } else {
                    val pitch = profile.hz?.let { Pitch.format(it) } ?: "뚜렷한 음 없음"
                    Text("$pitch · 크기 ${profile.loudness}", style = AppText.BodyStrong)
                }
            }
            if (profile == null) SmallPrimaryButton("찾기", onClick = onFind) else InlineAction("다시 찾기", onClick = onFind)
        }
        HorizontalDivider(color = Palette.Line)
        CheckInRow(checkIns, onCheckIn, onRecords)
    }
}

/** This week's check-in when it is due, otherwise the latest score and a way into 내 기록. */
@Composable
private fun CheckInRow(checkIns: List<CheckIn>, onCheckIn: () -> Unit, onRecords: () -> Unit) {
    val due = CheckIn.isDue(checkIns, LocalDate.now())
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = if (due) onCheckIn else onRecords)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (due) {
            Box(Modifier.size(8.dp).background(Palette.Teal, CircleShape))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (due) {
                val first = checkIns.isEmpty()
                Text(if (first) "첫 체크인" else "이번 주 체크인", style = AppText.Label)
                Text(if (first) "기준점을 만들어요 · 2분" else "질문 3개 · 2분", style = AppText.Caption, color = Palette.Ink2)
            } else {
                val last = checkIns.last()
                val previous = checkIns.getOrNull(checkIns.size - 2)
                Text("이명 점수 ${last.score}", style = AppText.Label)
                Text(
                    scoreChange(last.score, previous?.score, than = "지난주보다", same = "지난주와 같아요"),
                    style = AppText.Caption,
                    color = Palette.Ink2,
                )
            }
        }
        if (!due) Text("내 기록", style = AppText.Label, color = Palette.Teal)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
    }
}

/** Neutral wording for a score change; a rise is never coloured as an alarm. */
internal fun scoreChange(now: Int, before: Int?, than: String, same: String): String = when {
    before == null -> "첫 기록이에요"
    now < before -> "$than ${before - now}점 낮아요"
    now > before -> "$than ${now - before}점 높아요"
    else -> same
}

@Composable
private fun SoundPicker(selected: Sound, onSound: (Sound) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("노이즈")
        TileRow(Sound.noises, selected, onSound)
        Spacer(Modifier.size(8.dp))
        GroupLabel("자연음")
        TileRow(Sound.nature, selected, onSound)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = AppText.Caption, color = Palette.Ink2, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun TileRow(sounds: List<Sound>, selected: Sound, onSound: (Sound) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sounds.forEach { sound ->
            SoundTile(
                sound = sound,
                icon = sound.icon(),
                selected = sound == selected,
                onClick = { onSound(sound) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Nature sounds have an icon; noise tiles and rows show a colour swatch instead. Also used by the day sheet. */
internal fun Sound.icon(): ImageVector? = when (this) {
    Sound.RAIN -> Icons.Filled.Umbrella
    Sound.WAVES -> Icons.Filled.Waves
    Sound.BROOK -> Icons.Filled.WaterDrop
    Sound.FIRE -> Icons.Filled.LocalFireDepartment
    else -> null   // noise tiles show a colour swatch instead
}

@Composable
private fun Player(
    playing: Boolean,
    timer: PlaybackController.Timer,
    volume: Int,
    volumeMax: Int,
    mixingPoint: Int?,
    onPlay: () -> Unit,
    onVolume: (Int) -> Unit,
    onMix: () -> Unit,
    onTimer: () -> Unit,
) {
    // While dragging, follow the finger; the media volume itself only has whole steps.
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, Corners.Panel, ambientColor = Palette.Shadow, spotColor = Palette.Shadow)
            .background(Palette.Surface, Corners.Panel)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            AppSlider(
                value = dragging ?: volume.toFloat(),
                onValueChange = {
                    dragging = it
                    onVolume(it.roundToInt())
                },
                onValueChangeFinished = { dragging = null },
                description = "볼륨",
                valueRange = 0f..volumeMax.toFloat(),
                mark = mixingPoint?.let { it.toFloat() / volumeMax },
                markLabel = "혼합점",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TimerPill(timer, onTimer)
            Spacer(Modifier.weight(1f))
            PlayButton(playing = playing, onClick = onPlay, size = 72.dp, label = if (playing) "일시정지" else "재생")
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(PILL_WIDTH))   // keeps play centred
        }
        VolumeHint(mixingPoint, volume, onMix)
    }
}

private val PILL_WIDTH = 96.dp

@Composable
private fun TimerPill(timer: PlaybackController.Timer, onClick: () -> Unit) {
    val on = timer.active
    val label = when {
        !on -> "타이머"
        timer.remainingMs >= 60_000 -> "${ceil(timer.remainingMs / 60_000.0).toInt()}분"
        else -> "${ceil(timer.remainingMs / 1_000.0).toInt()}초"
    }
    Row(
        Modifier
            .size(width = PILL_WIDTH, height = 48.dp)
            .semantics { contentDescription = if (on) "수면 타이머, $label 남음" else "수면 타이머" }
            .tappable(
                Corners.Pill,
                if (on) Palette.TealSoft else Color.Transparent,
                border = if (on) BorderStroke(2.dp, Palette.Teal) else BorderStroke(1.dp, Palette.Line),
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Bedtime, contentDescription = null, tint = if (on) Palette.Teal else Palette.Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = AppText.Label, color = if (on) Palette.Teal else Palette.Ink2)
    }
}

/** Under the play button: find the mixing point, or say whether the volume sits below it. */
@Composable
private fun VolumeHint(mixingPoint: Int?, volume: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(Corners.Button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            mixingPoint == null -> {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
                Text("내 볼륨 찾기 · 1분", style = AppText.Label, color = Palette.Teal)
            }
            volume < mixingPoint -> {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
                Text("권장 볼륨이에요", style = AppText.Caption, color = Palette.Ink2)
            }
            else -> {
                Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = Palette.Ink2, modifier = Modifier.size(18.dp))
                Text("조금 커요 · 이명이 가려질 수 있어요", style = AppText.Caption, color = Palette.Ink2)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
    }
}
