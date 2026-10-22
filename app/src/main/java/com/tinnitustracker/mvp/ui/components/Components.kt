package com.tinnitustracker.mvp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tinnitustracker.mvp.data.Sound
import com.tinnitustracker.mvp.ui.theme.AppText
import com.tinnitustracker.mvp.ui.theme.Corners
import com.tinnitustracker.mvp.ui.theme.Palette

val RED_FLAGS = listOf(
    "한쪽 귀에서만 들려요",
    "맥박처럼 쿵쿵 뛰는 소리가 들려요",
    "어지럽거나 몸의 균형이 흔들려요",
    "갑자기 잘 안 들리게 됐어요",
    "얼굴 저림, 힘 빠짐, 심한 두통이 함께 있어요",
)

// ── Surfaces ─────────────────────────────────────────────────────────────────

/** A white card with a soft shadow and no border. */
fun Modifier.card(shape: Shape = Corners.Card, color: Color = Palette.Surface): Modifier =
    shadow(6.dp, shape, ambientColor = Palette.Shadow, spotColor = Palette.Shadow)
        .clip(shape)
        .background(color)

/** A surface that shrinks a little while pressed and ripples inside its own shape. */
fun Modifier.tappable(
    shape: Shape,
    color: Color,
    enabled: Boolean = true,
    elevated: Boolean = false,
    border: BorderStroke? = null,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 820f),
        label = "press",
    )
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(if (elevated) Modifier.shadow(6.dp, shape, ambientColor = Palette.Shadow, spotColor = Palette.Shadow) else Modifier)
        .clip(shape)
        .background(color)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .clickable(interaction, LocalIndication.current, enabled = enabled, role = role, onClick = onClick)
}

// ── Buttons ──────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .tappable(Corners.Button, Palette.Teal, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.BodyStrong, color = Palette.Surface)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .tappable(Corners.Button, Palette.Surface, border = BorderStroke(1.dp, Palette.Line), onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Palette.Ink2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = if (height < 52.dp) AppText.Label else AppText.BodyStrong)
    }
}

/** A text-only action in teal, full width, for the less likely choice above a primary button. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .tappable(Corners.Button, Color.Transparent, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.BodyStrong, color = Palette.Teal)
    }
}

/** A compact teal-text button that sits inside a card row. */
@Composable
fun InlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .tappable(Corners.Button, Color.Transparent, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.Label, color = Palette.Teal)
    }
}

/** A compact filled button for a card row. 40 dp tall, 48 dp touch target. */
@Composable
fun SmallPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(48.dp)
            .padding(vertical = 4.dp)
            .tappable(Corners.Button, Palette.Teal, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.Label, color = Palette.Surface)
    }
}

/** The one play button: coral, with a slow pulse while sound is on. */
@Composable
fun PlayButton(playing: Boolean, onClick: () -> Unit, size: Dp, label: String, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (playing) PulseRing(size)
        Box(
            Modifier
                .size(size)
                .shadow(8.dp, CircleShape, ambientColor = Palette.Coral, spotColor = Palette.Coral)
                .tappable(CircleShape, Palette.Coral, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Palette.Surface,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
private fun PulseRing(size: Dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing)),
        label = "pulse-progress",
    )
    Box(
        Modifier
            .size(size)
            .drawBehind {
                val radius = this.size.minDimension / 2 + 6.dp.toPx()
                drawCircle(
                    color = Palette.Coral.copy(alpha = 0.55f * (1 - p)),
                    radius = radius * (0.92f + 0.33f * p),
                    style = Stroke(width = 2.dp.toPx()),
                )
            },
    )
}

// ── Slider ───────────────────────────────────────────────────────────────────

/**
 * The app's slider. [mark] (0–1) draws a labelled tick on the track. With [showThumb] false
 * nothing looks selected until the first touch; [filled] false keeps the track one colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    mark: Float? = null,
    markLabel: String? = null,
    showThumb: Boolean = true,
    filled: Boolean = true,
    trackHeight: Dp = 6.dp,
) {
    val measurer = rememberTextMeasurer()
    val markStyle = AppText.Caption.copy(color = Palette.Ink2)
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            // The loudness thumb starts at the left edge, inside the system back-gesture zone.
            .systemGestureExclusion()
            .semantics { contentDescription = description },
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            if (showThumb) {
                Box(
                    Modifier
                        .size(24.dp)
                        .shadow(2.dp, CircleShape)
                        .background(Palette.Surface, CircleShape)
                        .border(2.dp, Palette.Teal, CircleShape),
                )
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span <= 0f) 0f else ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            val label = if (mark != null && markLabel != null) measurer.measure(markLabel, markStyle) else null
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight),
            ) {
                val corner = CornerRadius(size.height / 2)
                drawRoundRect(Palette.Track, cornerRadius = corner)
                if (filled) drawRoundRect(Palette.Teal, size = Size(size.width * fraction, size.height), cornerRadius = corner)
                if (mark != null) {
                    val x = size.width * mark.coerceIn(0f, 1f)
                    val center = size.height / 2
                    val half = 6.dp.toPx()
                    drawLine(Palette.Ink2, Offset(x, center - half), Offset(x, center + half), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    label?.let { drawText(it, topLeft = Offset(x - it.size.width / 2f, center + 9.dp.toPx())) }
                }
            }
        },
    )
}

/** A slider with a word under each end. */
@Composable
fun LabeledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    start: String,
    end: String,
    description: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    showThumb: Boolean = true,
    filled: Boolean = true,
    trackHeight: Dp = 6.dp,
) {
    Column(Modifier.fillMaxWidth()) {
        AppSlider(
            value, onValueChange, description,
            valueRange = valueRange, showThumb = showThumb, filled = filled, trackHeight = trackHeight,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(start, style = AppText.Caption, color = Palette.Ink2, modifier = Modifier.weight(1f))
            Text(end, style = AppText.Caption, color = Palette.Ink2, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

// ── Structure ────────────────────────────────────────────────────────────────

@Composable
fun AppBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    step: String? = null,
    large: Boolean = false,
    onRecords: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Palette.Ink2)
            }
        }
        Text(
            title.orEmpty(),
            style = if (large) AppText.Title else AppText.BodyStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 12.dp else 4.dp),
        )
        if (step != null) Text(step, style = AppText.Caption, color = Palette.Ink2, modifier = Modifier.padding(end = 16.dp))
        if (onRecords != null) {
            IconButton(onClick = onRecords) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "내 기록", tint = Palette.Ink2)
            }
        }
        if (onInfo != null) {
            IconButton(onClick = onInfo) {
                Icon(Icons.Filled.Info, contentDescription = "앱 정보", tint = Palette.Ink2)
            }
        }
    }
}

/** App bar, scrolling content and a footer that stays above the navigation bar. */
@Composable
fun StepScaffold(
    title: String? = null,
    step: String? = null,
    onBack: (() -> Unit)? = null,
    centered: Boolean = false,
    topPadding: Dp = 8.dp,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .statusBarsPadding(),
    ) {
        if (title != null || onBack != null) AppBar(title = title, onBack = onBack, step = step)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = if (centered) 32.dp else topPadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            content = content,
        )
        if (footer != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = footer,
            )
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun Heading(title: String, body: String, center: Boolean = false) {
    val align = if (center) TextAlign.Center else TextAlign.Start
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(title, style = AppText.Title, textAlign = align)
        Text(body, style = AppText.Body, color = Palette.Ink2, textAlign = align)
    }
}

@Composable
fun Badge(icon: ImageVector, tint: Color, background: Color) {
    Box(
        Modifier
            .size(64.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun AppMark() {
    Box(
        Modifier
            .size(48.dp)
            .background(Palette.Teal, Corners.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Palette.Surface, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun HintChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .height(32.dp)
            .background(Palette.Surface, Corners.Pill)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
        Text(text, style = AppText.Caption, color = Palette.Ink2)
    }
}

/** A big number with a caption and, when [onToggle] is given, a play button. */
@Composable
fun Readout(value: String, caption: String, playing: Boolean = false, onToggle: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.TealSoft, Corners.Card)
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = AppText.Display, color = Palette.Teal)
        Spacer(Modifier.height(4.dp))
        Text(caption, style = AppText.Caption, color = Palette.Ink2)
        if (onToggle != null) {
            Spacer(Modifier.height(16.dp))
            PlayButton(playing = playing, onClick = onToggle, size = 64.dp, label = if (playing) "소리 끄기" else "소리 켜기")
        }
    }
}

/** "조금 낮게 / 조금 높게" style fine-tuning buttons. */
@Composable
fun Steppers(minus: String, plus: String, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(minus, { onStep(-1) }, Modifier.weight(1f), icon = Icons.Filled.Remove, height = 48.dp)
        SecondaryButton(plus, { onStep(1) }, Modifier.weight(1f), icon = Icons.Filled.Add, height = 48.dp)
    }
}

/** One of the octave-check rows: tap to hear it and pick it. */
@Composable
fun ChoiceRow(title: String, detail: String, selected: Boolean, playing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .tappable(
                Corners.Card,
                if (selected) Palette.TealSoft else Palette.Surface,
                elevated = !selected,
                border = if (selected) BorderStroke(2.dp, Palette.Teal) else null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = 72.dp)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(if (playing) Palette.Coral else Palette.CoralSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (playing) Palette.Surface else Palette.Coral,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.BodyStrong)
            Text(detail, style = AppText.Caption, color = Palette.Ink2)
        }
        RadioDot(selected)
    }
}

@Composable
fun RadioDot(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .border(2.dp, if (selected) Palette.Teal else Palette.Muted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(Palette.Teal, CircleShape))
    }
}

/** One of the seven sound choices. */
@Composable
fun SoundTile(sound: Sound, icon: ImageVector?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(76.dp)
            .semantics {
                this.selected = selected
                contentDescription = sound.fullName
            }
            .tappable(
                Corners.Button,
                if (selected) Palette.TealSoft else Palette.Surface,
                border = if (selected) BorderStroke(2.dp, Palette.Teal) else null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Palette.Teal else Palette.Ink2)
        } else {
            Box(
                Modifier
                    .size(24.dp)
                    .background(Palette.Swatches.getValue(sound.id), CircleShape)
                    .border(1.dp, Palette.Ink.copy(alpha = 0.14f), CircleShape),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(sound.label, style = AppText.Label, color = if (selected) Palette.Teal else Palette.Ink)
    }
}

/** A whole card that works as an on/off switch. */
@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (checked) "켜짐" else "꺼짐" }
            .tappable(Corners.Card, Palette.Surface, enabled = enabled, elevated = true, role = Role.Switch, onClick = onToggle)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.BodyStrong, color = if (enabled) Palette.Ink else Palette.Muted)
            Text(subtitle, style = AppText.Caption, color = Palette.Ink2)
        }
        Spacer(Modifier.width(12.dp))
        SwitchTrack(checked = checked, enabled = enabled)
    }
}

@Composable
private fun SwitchTrack(checked: Boolean, enabled: Boolean) {
    val track by animateColorAsState(if (checked) Palette.Teal else Palette.SwitchOff, label = "switch-track")
    val knobX by animateDpAsState(if (checked) 24.dp else 4.dp, label = "switch-knob")
    Box(
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(track, Corners.Pill),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(knobX.roundToPx(), 0) }
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .background(Palette.Surface, CircleShape),
        )
    }
}

/** The red-flag list. Its wording is never softened (docs/clinical/red-flags.md). */
@Composable
fun SafetyBox(title: String = "이런 경우엔 먼저 진료를 받으세요") {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.WarnSoft, Corners.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.Warn)
            Spacer(Modifier.width(8.dp))
            Text(title, style = AppText.Body.copy(fontWeight = AppText.Title.fontWeight))
        }
        Spacer(Modifier.height(4.dp))
        RED_FLAGS.forEach { flag ->
            Row {
                Box(
                    Modifier
                        .padding(start = 4.dp, top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .background(Palette.Warn, CircleShape),
                )
                Text(flag, style = AppText.Body)
            }
        }
    }
}

@Composable
fun WarnNote(text: AnnotatedString) {
    Text(
        text,
        style = AppText.Body,
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.WarnSoft, Corners.Card)
            .padding(16.dp),
    )
}

/** A short confirmation that slides in above the player and fades away. */
@Composable
fun MessageToast(message: String?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf("") }
    if (message != null) last = message
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
    ) {
        Text(
            last,
            style = AppText.Body.copy(fontSize = AppText.Label.fontSize, lineHeight = AppText.Label.lineHeight),
            color = Palette.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Ink, Corners.Chip)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
