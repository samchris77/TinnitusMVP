# TinnitusMVP

The minimal two-function app from [`docs/design/minimal-redesign.html`](docs/design/minimal-redesign.html):

1. **내 이명 찾기**: find the tinnitus pitch (slider plus semitone steps), rule out an octave mix-up, then match the loudness starting from silence.
2. **소리 듣기**: white, pink or brown noise, or one of four nature sounds, with an optional one-octave notch at the measured pitch. Playback continues with the screen off and has a notification.

v1.1, built from [`docs/design/v1.1-design.html`](docs/design/v1.1-design.html), adds three things:

- **내 볼륨 찾기:** find the mixing point and set the volume one step below it. The volume slider then shows a 혼합점 mark.
- **주간 체크인:** a weekly K-TMI check-in (validated Korean wording and weights), with a 내 변화 screen showing the score, a trend chart and the history.
- **수면 타이머:** 15, 30, 60 or 120 minutes, with a 30-second fade at the end. It counts only while sound plays.

v1.2, built from [`docs/design/v1.2-calendar.html`](docs/design/v1.2-calendar.html), adds a listening calendar:

- **내 기록** (내 변화 renamed) opens from a calendar icon in the main app bar, in any week. A month calendar sits on top; the v1.1 score, chart and history follow below.
- Listening time is recorded automatically while sound plays: per day, sound and notch setting. Days are shaded in three steps (under 30분, 30분–2시간, 2시간 이상), and check-in days get a dot. Tapping a day shows what played and for how long.
- Counting rules: saved every minute and on pause, so a killed app loses at most a minute. A session under a minute is dropped. A session counts toward the day it started, and the day changes at 4 a.m. The finder's tones never count.
- There is no daily goal and no streak. Listening is only recorded from the day of the update.

## Build and run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug :app:testDebugUnitTest
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Toolchain: AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, Compose BOM 2025.08.00. minSdk 28, targetSdk 36.

## Layout

| Package | What it holds |
|---|---|
| `audio/` | `Mixer` (pure Kotlin: sources, crossfades, band-stop, finder probe), `Dsp` (biquads, noise, loops), `SoundEngine` (AudioTrack thread that parks when silent), `NatureDecoder`, `PlaybackController` (focus, unplug, state), `PlaybackService` + `Notifications`, `MediaVolume` |
| `data/` | `Prefs` (DataStore, one file), `Sound`, `CheckIn` (K-TMI score, due rule, compact encoding), `ListeningLog` (listening entries, the 4 a.m. rule, month totals) |
| `ui/` | `AppViewModel` (screens and finder state), `App` (navigation), `screens/`, `components/`, `theme/` |
| `util/` | `Pitch`: pitch and loudness maths |

Unit tests in `app/src/test` (29) cover the pitch maths, the DSP (notch depth, noise levels, seamless loops, the sleep-timer fade), the check-in model (score, encoding, due dates) and the listening log (encoding, the 4 a.m. rule, merging, shading, month totals).

## What's in this folder

Everything the app needs is here; nothing outside `TinnitusMVP/` is used.

| Path | What it holds |
|---|---|
| `app/` | The Android app. The four nature loops are in `app/src/main/res/raw/`. |
| `sounds/source/` | The original nature recordings (CC0, lossless FLAC, 172 MB) the loops are cut from. See [`sounds/README.md`](sounds/README.md). |
| `tools/` | `make_loops.sh` rebuilds the loops from `sounds/source/` (macOS, uses `afconvert` and `python3`). |
| `docs/design/` | The design files (HTML): `minimal-redesign`, `v1.1-design`, `v1.2-calendar`, `palette`, `airy-palettes`, `apricot-palette`, `slate-palette` (colour proposals), `layout-optimization` (layout audit). |
| `docs/clinical/` | Snapshots of the clinical notes the design and code cite: red flags, sound therapy, K-TMI. |

To view the design files with working sounds, serve this folder and open `http://localhost:8766/docs/design/minimal-redesign.html`:

```bash
python3 -m http.server 8766
```

## Audio files

`app/src/main/res/raw/*.m4a` are two-minute stereo AAC loops (160 kbps, 48 kHz) cut from CC0 field recordings in `sounds/source/`, levelled to about −26 dBFS with short peaks limited. Together they are 9.9 MB. The app plays them in stereo and cross-fades the loop point (2 s) when it decodes them. Sources and licences are in [`sounds/README.md`](sounds/README.md). To rebuild them (needs `python3` with numpy):

```bash
tools/make_loops.sh
```

## Placeholders before a Play release

- App name "이명 사운드" and application id `com.tinnitustracker.mvp`. Both are permanent after the first upload.
- Privacy policy URL and support contact. The 앱 정보 rows show a "데모" toast for now.
- A release signing config. `assembleRelease` builds an unsigned APK.
- Clinical review of the Korean safety, disclaimer and listening-hint wording.
