package com.tinnitustracker.mvp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colours from docs/design/minimal-redesign.html. Coral is for play buttons only. */
object Palette {
    val Bg = Color(0xFFF5F1EA)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1A2E2C)
    val Ink2 = Color(0xFF4A5C5A)
    /** Icons and disabled text only: too faint for readable text. */
    val Muted = Color(0xFF8B9694)
    val Line = Color(0xFFE5DED2)
    val Track = Color(0xFFE2DCD1)
    val SwitchOff = Color(0xFFD9D3C8)
    val Teal = Color(0xFF2D5F5D)
    val TealSoft = Color(0xFFE8F0EE)
    val Coral = Color(0xFFD67C5C)
    val CoralSoft = Color(0xFFF8E4D9)
    val Warn = Color(0xFFC44536)
    val WarnSoft = Color(0xFFF7DDD8)
    val Shadow = Color(0x291A2E2C)
    val Swatches = mapOf("white" to Color(0xFFFFFFFF), "pink" to Color(0xFFEBB7B1), "brown" to Color(0xFF8C664D))
}

/** The five text styles. Nothing is smaller than 13 sp. */
object AppText {
    // Korean text breaks between words, not inside them, where the platform supports it.
    private val korean = TextStyle(
        localeList = LocaleList("ko-KR"),
        lineBreak = LineBreak(
            strategy = LineBreak.Strategy.HighQuality,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase,
        ),
    )

    val Display = korean.merge(TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
    val Title = korean.merge(TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp))
    val Body = korean.merge(TextStyle(fontSize = 16.sp, lineHeight = 24.sp))
    val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
    val Label = korean.merge(TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold))
    val Caption = korean.merge(TextStyle(fontSize = 13.sp, lineHeight = 18.sp))
}

object Corners {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Pill = RoundedCornerShape(100)
    val Panel = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}

@Composable
fun MvpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Teal,
            onPrimary = Palette.Surface,
            background = Palette.Bg,
            onBackground = Palette.Ink,
            surface = Palette.Surface,
            onSurface = Palette.Ink,
            error = Palette.Warn,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Palette.Ink, content = content)
    }
}
