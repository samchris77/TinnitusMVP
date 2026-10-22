package com.tinnitustracker.mvp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** What the finder measured. [hz] is null when the tinnitus has no clear tone. */
data class Profile(val hz: Float?, val loudness: Int, val measuredAt: Long)

data class Settings(
    val onboarded: Boolean = false,
    val profile: Profile? = null,
    val sound: Sound = Sound.PINK,
    val notchOn: Boolean = false,
    /** Media-volume step where the sound starts to blend with the tinnitus. */
    val mixingPoint: Int? = null,
    val checkIns: List<CheckIn> = emptyList(),
)

/** Everything the app remembers, in one small DataStore file on the phone. */
class Prefs(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarding_complete")
        val HZ = floatPreferencesKey("matched_frequency_hz")
        val TONAL = booleanPreferencesKey("has_tonal_tinnitus")
        val LOUDNESS = intPreferencesKey("tinnitus_loudness")
        val MEASURED_AT = longPreferencesKey("measured_at")
        val SOUND = stringPreferencesKey("sound_id")
        val NOTCH = booleanPreferencesKey("notch_on")
        val MIX = intPreferencesKey("mixing_point_level")
        val CHECKINS = stringPreferencesKey("checkins")
    }

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            Settings(
                onboarded = p[Keys.ONBOARDED] ?: false,
                profile = p[Keys.LOUDNESS]?.let { loudness ->
                    Profile(
                        hz = if (p[Keys.TONAL] == false) null else p[Keys.HZ],
                        loudness = loudness,
                        measuredAt = p[Keys.MEASURED_AT] ?: 0L,
                    )
                },
                sound = Sound.of(p[Keys.SOUND]),
                notchOn = p[Keys.NOTCH] ?: false,
                mixingPoint = p[Keys.MIX],
                checkIns = CheckIn.decode(p[Keys.CHECKINS]),
            )
        }

    suspend fun saveMixingPoint(level: Int) {
        store.edit { it[Keys.MIX] = level }
    }

    suspend fun addCheckIn(entry: CheckIn) {
        store.edit { it[Keys.CHECKINS] = CheckIn.encode(CheckIn.add(CheckIn.decode(it[Keys.CHECKINS]), entry)) }
    }

    suspend fun setOnboarded() {
        store.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun saveProfile(profile: Profile, notchOn: Boolean) {
        store.edit {
            it[Keys.TONAL] = profile.hz != null
            if (profile.hz != null) it[Keys.HZ] = profile.hz else it.remove(Keys.HZ)
            it[Keys.LOUDNESS] = profile.loudness
            it[Keys.MEASURED_AT] = profile.measuredAt
            it[Keys.NOTCH] = notchOn
        }
    }

    suspend fun setSound(sound: Sound) {
        store.edit { it[Keys.SOUND] = sound.id }
    }

    suspend fun setNotch(on: Boolean) {
        store.edit { it[Keys.NOTCH] = on }
    }
}
