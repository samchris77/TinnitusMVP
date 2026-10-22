#!/bin/sh
# Rebuilds app/src/main/res/raw/*.m4a from the recordings in sounds/source/ (macOS: uses afconvert).
# Each loop: stereo, 48 kHz, the steadiest 2 minutes of the recording, levelled to about -26 dBFS
# with short peaks limited to -1 dBFS, AAC 160 kbps. Needs python3 with numpy.
#
# usage: tools/make_loops.sh [OUT_DIR]     (default: app/src/main/res/raw)
set -eu

ROOT=$(cd "$(dirname "$0")/.." && pwd)
SRC="$ROOT/sounds/source"
OUT="${1:-$ROOT/app/src/main/res/raw}"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT"

make() {   # make <name> <freesound id> [start seconds]
  src=$(ls "$SRC"/"$2"__* 2>/dev/null | head -n 1)
  [ -n "$src" ] || { echo "missing: $SRC/$2__* (see sounds/README.md)" >&2; exit 1; }
  afconvert -f WAVE -d LEI16@48000 -c 2 "$src" "$TMP/$1_full.wav"
  python3 "$ROOT/tools/make_loop.py" "$TMP/$1_full.wav" "$TMP/$1.wav" --length 120 --start "${3:-auto}"
  afconvert -f m4af -d aac -b 160000 "$TMP/$1.wav" "$OUT/$1.m4a"
}

# name   Freesound id   (optional fixed start, in seconds, to override the automatic pick)
make rain  136540
make waves 328313
make brook 220528
make fire  40699
ls -l "$OUT"/*.m4a
