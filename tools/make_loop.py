"""Cut the steadiest stretch of a field recording and level it, for the app's nature-sound loops.

usage: python3 make_loop.py IN.wav OUT.wav [--length 120] [--start auto|SECONDS] [--rms 0.05]

IN.wav must be 16-bit PCM (make_loops.sh converts with afconvert first); mono or stereo.
OUT.wav is 16-bit stereo at the same rate.

Picking the stretch (--start auto): the recording is measured in 100 ms blocks, and every start
point a second apart is scored on how much the level moves (standard deviation in dB) plus a
penalty for each block that jumps 6 dB or more above the median: pops, gusts, a car. Blocks that
are louder and far more tonal than the recording's usual texture (a bird, an insect, a voice)
cost much more. Sound therapy wants sound that doesn't catch the ear, so the calmest stretch wins. The first and
last few seconds are skipped (handling noise, fades).

Processing: a 25 Hz high-pass removes wind rumble that would only use up headroom, then one gain
brings the RMS (both channels) to --rms. A peak limiter then holds short peaks (fire crackles,
close drops) at -1 dBFS, turning them down by at most --max-limit-db; if that isn't enough, the
whole loop is turned down instead. Without the limiter, one crackle would set the level of the
whole fire loop.
The app cross-fades the loop point itself when it decodes the file.
"""
import argparse
import math
import wave

try:
    import numpy as np
except ImportError:  # pragma: no cover
    raise SystemExit("make_loop.py needs numpy: python3 -m pip install numpy")

BLOCK_S = 0.1
EDGE_S = 3.0
EVENT_DB = 6.0
EVENT_WEIGHT = 0.15
TONAL_FRACTION = 0.35
TONAL_DB = 3.0
TONAL_WEIGHT = 1.0
PEAK_CEILING = 10 ** (-1 / 20)


def read(path):
    with wave.open(path, "rb") as w:
        assert w.getsampwidth() == 2, "expects 16-bit PCM"
        ch, rate, n = w.getnchannels(), w.getframerate(), w.getnframes()
        x = np.frombuffer(w.readframes(n), dtype="<i2").astype(np.float64) / 32768
    x = x.reshape(-1, ch)
    if ch == 1:
        x = np.repeat(x, 2, axis=1)
    return x[:, :2], rate


def block_db(x, rate):
    b = int(rate * BLOCK_S)
    m = len(x) // b
    e = (x[: m * b] ** 2).reshape(m, b, 2).mean(axis=(1, 2))
    return 10 * np.log10(np.maximum(e, 1e-12))


def block_flatness(x, rate):
    """Spectral flatness of each 100 ms block, 300 Hz-10 kHz: near 1 for rain-like noise, near 0 for a call."""
    b = int(rate * BLOCK_S)
    m = len(x) // b
    mid = x[: m * b].mean(axis=1).reshape(m, b) * np.hanning(b)
    freqs = np.fft.rfftfreq(b, 1 / rate)
    band = (freqs > 300) & (freqs < 10000)
    p = np.abs(np.fft.rfft(mid, axis=1))[:, band] ** 2 + 1e-20
    return np.exp(np.log(p).mean(axis=1)) / p.mean(axis=1)


def pick_start(x, rate, length_s):
    db = block_db(x, rate)
    flat = block_flatness(x, rate)
    # A block that is louder than its surroundings and much more tonal than the recording's
    # usual texture: a bird, an insect, a voice. These catch the ear, so they cost the most.
    calls = (flat < np.median(flat) * TONAL_FRACTION) & (db > np.median(db) + TONAL_DB)
    per_s = int(round(1 / BLOCK_S))
    win = int(length_s * per_s)
    first = int(EDGE_S * per_s)
    last = len(db) - int(EDGE_S * per_s) - win
    if last < first:
        raise SystemExit(f"recording is too short for a {length_s:.0f} s loop")
    best = None
    for s in range(first, last + 1, per_s):
        w = db[s : s + win]
        med = np.median(w)
        jumps = int(np.count_nonzero(w > med + EVENT_DB))
        tonal = int(np.count_nonzero(calls[s : s + win]))
        score = w.std() + EVENT_WEIGHT * jumps + TONAL_WEIGHT * tonal
        if best is None or score < best[0]:
            best = (score, s, w.std(), jumps, tonal)
    _, s, std, jumps, tonal = best
    return s * BLOCK_S, std, jumps, tonal


def highpass(x, rate, hz=25.0):
    """2nd-order Butterworth high-pass, run forwards (RBJ cookbook)."""
    w0 = 2 * math.pi * hz / rate
    alpha = math.sin(w0) / (2 / math.sqrt(2))
    c = math.cos(w0)
    a0 = 1 + alpha
    b0, b1, b2 = (1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0
    a1, a2 = -2 * c / a0, (1 - alpha) / a0
    y = np.empty_like(x)
    for ch in range(x.shape[1]):
        z1 = z2 = 0.0
        xs = x[:, ch].tolist()   # plain floats: much faster in a Python loop than numpy scalars
        ys = [0.0] * len(xs)
        for i, v in enumerate(xs):
            out = b0 * v + z1
            z1 = b1 * v - a1 * out + z2
            z2 = b2 * v - a2 * out
            ys[i] = out
        y[:, ch] = ys
    return y


def limit(x, rate, ceiling, max_db, lookahead_s=0.002, release_s=0.08):
    """Peak limiter: lowers short peaks (crackles, drops) to [ceiling], by at most [max_db].

    The gain each sample needs is held over a 2 ms look-ahead, so it is already down when a
    peak arrives, and recovers over about 80 ms. Returns the limited signal and the most it
    turned down, in dB.
    """
    floor = 10 ** (-max_db / 20)
    level = np.abs(x).max(axis=1)
    need = np.clip(ceiling / np.maximum(level, 1e-9), floor, 1.0)
    # Look-ahead: each sample takes the smallest gain needed in the next few milliseconds.
    look = max(1, int(lookahead_s * rate))
    padded = np.concatenate([need, np.ones(look)])
    windows = np.lib.stride_tricks.sliding_window_view(padded, look + 1)
    need = windows.min(axis=1)[: len(level)]
    # Instant attack, smooth release.
    k = 1 - math.exp(-1 / (release_s * rate))
    g = [0.0] * len(need)
    cur = 1.0
    for i, n in enumerate(need.tolist()):
        cur = n if n < cur else cur + (n - cur) * k
        g[i] = cur
    g = np.asarray(g)
    return x * g[:, None], -20 * math.log10(float(g.min()))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("src")
    p.add_argument("dst")
    p.add_argument("--length", type=float, default=120.0, help="seconds to keep")
    p.add_argument("--start", default="auto", help="'auto' or seconds into the recording")
    p.add_argument("--rms", type=float, default=0.05, help="target RMS, 0.05 = about -26 dBFS")
    p.add_argument("--max-limit-db", type=float, default=8.0, help="most the peak limiter may turn a peak down")
    a = p.parse_args()

    x, rate = read(a.src)
    if a.start == "auto":
        start, std, jumps, tonal = pick_start(x, rate, a.length)
        how = f"steadiest stretch: level spread {std:.1f} dB, {jumps} jumps, {tonal} tonal blocks"
    else:
        start, how = float(a.start), "start given"
    i0 = int(start * rate)
    seg = x[i0 : i0 + int(a.length * rate)]
    if len(seg) < int(a.length * rate):
        raise SystemExit("recording ends before the loop does")
    # Filter a little before the cut so the filter has settled by the first kept sample.
    pre = min(i0, rate)
    seg = highpass(x[i0 - pre : i0 + len(seg)], rate)[pre:]

    rms = math.sqrt(float((seg ** 2).mean()))
    gain = a.rms / rms
    seg, reduced = limit(seg * gain, rate, PEAK_CEILING, a.max_limit_db)
    peak = float(np.abs(seg).max())
    if peak > PEAK_CEILING:   # more than the limiter may take: turn the whole loop down instead
        seg *= PEAK_CEILING / peak
        gain *= PEAK_CEILING / peak
    out = np.clip(np.round(seg * 32768), -32767, 32767).astype("<i2")
    final =math.sqrt(float((out.astype(np.float64) ** 2).mean())) / 32768
    with wave.open(a.dst, "wb") as o:
        o.setnchannels(2)
        o.setsampwidth(2)
        o.setframerate(rate)
        o.writeframes(out.tobytes())
    peak = float(np.abs(out).max()) / 32768
    print(f"{a.dst}: {start:.0f}-{start + a.length:.0f} s ({how}), gain {gain:.2f}, "
          f"limiter up to {reduced:.1f} dB, rms {20 * math.log10(final):.1f} dBFS, peak {20 * math.log10(peak):.1f} dBFS")


if __name__ == "__main__":
    main()
