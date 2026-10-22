# Nature sounds

`source/` holds the original field recordings. The app ships two-minute stereo loops cut from them, in `app/src/main/res/raw/`. Rebuild the loops (macOS, needs `python3` with numpy) with:

```bash
tools/make_loops.sh
```

The script picks the steadiest two minutes of each recording on its own, avoiding level jumps and tonal sounds such as birds (see `tools/make_loop.py`); give a start time as the third argument in `make_loops.sh` to override it. It prints what it chose and what it found. The current picks: rain 0 jumps / 0 tonal blocks, stream 0 / 0, waves 39 jumps (breaking waves) / 2 low thumps, fire 7 / 5 low crackle thumps (300–450 Hz).

## Recordings

All four are **CC0 (public domain)**: free to use, change and ship in a commercial app, with no credit required. Credit is still given in the app's 앱 정보 sheet as a courtesy.

| App file | Recording | Recordist | Original | Licence |
|---|---|---|---|---|
| `rain.m4a` | [AutumnRainWithTingNov2011](https://freesound.org/people/kvgarlic/sounds/136540/) (seconds 5–125; the "ting" near the end is left out) | kvgarlic | WAV, 96 kHz, 16-bit, stereo, 2:13 | CC0 |
| `waves.m4a` | [Waves from the sea on the beach in Punta Allen, Sian Ka'an Biosphere Reserve](https://freesound.org/people/felix.blume/sounds/328313/) (Mexico) | Felix Blume | WAV, 96 kHz, 24-bit, stereo, 3:00 | CC0 |
| `brook.m4a` | [Stream Wyre forest 8.1.14](https://freesound.org/people/turbostream/sounds/220528/) (England) | turbostream | FLAC, 44.1 kHz, 16-bit, stereo, 6:30 | CC0 |
| `fire.m4a` | [campfire.wav](https://freesound.org/people/Spandau/sounds/40699/) | Spandau | WAV, 48 kHz, 24-bit, stereo, 3:56 | CC0 |

Downloaded from Freesound on 2026-10-22. The three WAVs are stored here as FLAC, which is lossless (checked: identical samples) and about 40 % smaller; the stream was FLAC already. Files keep Freesound's names (`<id>__<user>__<title>`); `make_loops.sh` finds them by id.

Rejected: [Light Rain - Jungle Rain](https://freesound.org/people/florianreichelt/sounds/422052/) has bird or insect calls (about 1.3 kHz, up to 15 dB above the rain) all through its 8 minutes. Backups, also CC0: waves [243670](https://freesound.org/people/Bruno_Garcez/sounds/243670/), stream [197023](https://freesound.org/people/Yuval/sounds/197023/) (binaural), fire [809834](https://freesound.org/people/rozhkovkirill/sounds/809834/).

## Previous recordings

`previous/` (local only, kept out of git) holds the four recordings the first loops were cut from. Their file names look like Pixabay downloads, but their source pages and licences were never recorded, so they are no longer used or published. Pixabay's terms forbid distributing content "on a standalone basis", and it's unclear whether a sound app counts. They can be deleted.
