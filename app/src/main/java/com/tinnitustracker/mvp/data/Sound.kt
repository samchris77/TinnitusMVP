package com.tinnitustracker.mvp.data

import androidx.annotation.RawRes
import com.tinnitustracker.mvp.R

/** The seven sounds. Noise is generated; nature sounds are two-minute stereo loops in res/raw. */
enum class Sound(
    val id: String,
    val label: String,
    val fullName: String,
    @RawRes val rawRes: Int? = null,
) {
    WHITE("white", "화이트", "화이트 노이즈"),
    PINK("pink", "핑크", "핑크 노이즈"),
    BROWN("brown", "브라운", "브라운 노이즈"),
    RAIN("rain", "빗소리", "빗소리", R.raw.rain),
    WAVES("waves", "파도", "파도", R.raw.waves),
    BROOK("brook", "시냇물", "시냇물", R.raw.brook),
    FIRE("fire", "모닥불", "모닥불", R.raw.fire);

    val isNoise: Boolean get() = rawRes == null

    companion object {
        val noises: List<Sound> = entries.filter { it.isNoise }
        val nature: List<Sound> = entries.filter { !it.isNoise }

        fun of(id: String?): Sound = entries.firstOrNull { it.id == id } ?: PINK
    }
}
