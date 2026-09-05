"""Generate the original Road Rooster gameplay music loop.

The track is synthesized from scratch: no samples, copied melodies, or external
assets are used. Run this script with Python + NumPy, then encode the WAV as Ogg
Vorbis for Android's res/raw directory.
"""

from __future__ import annotations

import math
import wave
from pathlib import Path

import numpy as np


SAMPLE_RATE = 44_100
BPM = 120
BEAT = 60.0 / BPM
BARS = 16
BAR = BEAT * 4
DURATION = BARS * BAR
FRAME_COUNT = int(DURATION * SAMPLE_RATE)
RNG = np.random.default_rng(20_260_902)

mix = np.zeros((FRAME_COUNT, 2), dtype=np.float64)


def midi(note: float) -> float:
    return 440.0 * 2.0 ** ((note - 69.0) / 12.0)


def pan_gains(pan: float) -> tuple[float, float]:
    angle = (pan + 1.0) * math.pi / 4.0
    return math.cos(angle), math.sin(angle)


def add_signal(start: float, signal: np.ndarray, gain: float, pan: float = 0.0) -> None:
    first = max(0, int(start * SAMPLE_RATE))
    last = min(FRAME_COUNT, first + signal.size)
    if last <= first:
        return
    left, right = pan_gains(pan)
    section = signal[: last - first] * gain
    mix[first:last, 0] += section * left
    mix[first:last, 1] += section * right


def envelope(length: int, attack: float, release: float, decay: float = 0.0) -> np.ndarray:
    t = np.arange(length) / SAMPLE_RATE
    env = np.ones(length)
    if attack > 0.0:
        env *= np.minimum(1.0, t / attack)
    if decay > 0.0:
        env *= np.exp(-t / decay)
    if release > 0.0:
        remaining = (length - np.arange(length)) / SAMPLE_RATE
        env *= np.minimum(1.0, remaining / release)
    return env


def add_banjo(start: float, note: int, duration: float, gain: float, pan: float) -> None:
    length = int(duration * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    frequency = midi(note)
    phase = 2.0 * math.pi * frequency * t
    bright = (
        np.sin(phase)
        + 0.52 * np.sin(phase * 2.01)
        + 0.25 * np.sin(phase * 3.02)
        + 0.12 * np.sin(phase * 5.03)
    )
    pick = RNG.normal(0.0, 1.0, length) * np.exp(-t / 0.013)
    signal = (bright * 0.44 + pick * 0.12) * envelope(length, 0.002, 0.045, 0.23)
    add_signal(start, signal, gain, pan)


def add_bass(start: float, note: int, duration: float, gain: float) -> None:
    length = int(duration * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    frequency = midi(note)
    phase = 2.0 * math.pi * frequency * t
    signal = (np.sin(phase) + 0.24 * np.sin(phase * 2.0)) * envelope(length, 0.012, 0.10, 0.8)
    add_signal(start, signal, gain, -0.08)


def add_lead(start: float, note: int, duration: float, gain: float, pan: float) -> None:
    length = int(duration * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    frequency = midi(note)
    vibrato_phase = 2.0 * math.pi * frequency * t + 0.025 * np.sin(2.0 * math.pi * 5.2 * t)
    signal = (
        np.sin(vibrato_phase)
        + 0.22 * np.sin(vibrato_phase * 2.0)
        + 0.08 * np.sin(vibrato_phase * 3.0)
    ) * envelope(length, 0.018, 0.07)
    add_signal(start, signal, gain, pan)


def add_kick(start: float, gain: float = 1.0) -> None:
    length = int(0.28 * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    phase = 2.0 * math.pi * (48.0 * t + 54.0 * (1.0 - np.exp(-t / 0.025)) * 0.025)
    body = np.sin(phase) * np.exp(-t / 0.095)
    click = RNG.normal(0.0, 1.0, length) * np.exp(-t / 0.009) * 0.10
    add_signal(start, body + click, 0.42 * gain)


def add_snare(start: float, gain: float = 1.0) -> None:
    length = int(0.24 * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    noise = RNG.normal(0.0, 1.0, length)
    high = np.concatenate(([noise[0]], np.diff(noise)))
    body = np.sin(2.0 * math.pi * 185.0 * t) * np.exp(-t / 0.075)
    signal = high * np.exp(-t / 0.070) * 0.28 + body * 0.18
    add_signal(start, signal, 0.34 * gain, 0.08)


def add_shaker(start: float, pan: float) -> None:
    length = int(0.075 * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    noise = RNG.normal(0.0, 1.0, length)
    high = np.concatenate(([noise[0]], np.diff(noise)))
    signal = high * np.sin(math.pi * np.minimum(1.0, t / 0.008)) * np.exp(-t / 0.027)
    add_signal(start, signal, 0.075, pan)


def add_cowbell(start: float, note: int, gain: float = 1.0) -> None:
    length = int(0.20 * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    frequency = midi(note)
    signal = (
        np.sin(2.0 * math.pi * frequency * t)
        + 0.72 * np.sin(2.0 * math.pi * frequency * 1.49 * t)
    ) * envelope(length, 0.001, 0.035, 0.075)
    add_signal(start, signal, 0.10 * gain, 0.24)


chords = [
    ([55, 59, 62, 67], 43),  # G
    ([50, 54, 57, 62], 38),  # D
    ([52, 55, 59, 64], 40),  # Em
    ([48, 52, 55, 60], 36),  # C
]

melodies = [
    [67, 71, 74, 71, 69, 67, 66, 67],
    [69, 74, 78, 76, 74, 69, 66, 69],
    [71, 76, 79, 78, 76, 74, 71, 67],
    [72, 76, 79, 76, 74, 72, 71, 67],
]

for bar_index in range(BARS):
    bar_start = bar_index * BAR
    chord, bass_root = chords[bar_index % len(chords)]
    phrase = bar_index // 4

    # Alternating root/fifth bass gives forward motion without a boost-loop feel.
    for beat_index in range(4):
        bass_note = bass_root if beat_index in (0, 2) else bass_root + 7
        add_bass(bar_start + beat_index * BEAT, bass_note, BEAT * 0.82, 0.31)

    # Eight-note farm plucks, spread gently across the stereo field.
    arpeggio = [0, 2, 1, 2, 0, 3, 1, 2]
    for step, chord_index in enumerate(arpeggio):
        note = chord[chord_index]
        if phrase >= 2 and step in (3, 7):
            note += 12
        add_banjo(
            bar_start + step * BEAT / 2.0,
            note,
            BEAT * 0.42,
            0.25 if step % 2 == 0 else 0.21,
            -0.32 if step % 2 == 0 else 0.26,
        )

    # Original call-and-response melody. The opening is intentionally lighter,
    # then the last eight bars become more celebratory.
    melody = melodies[bar_index % 4]
    for step, note in enumerate(melody):
        if phrase == 0 and step % 2 == 1:
            continue
        if phrase == 1 and step in (2, 6):
            continue
        octave = 12 if phrase == 3 and step in (0, 4) else 0
        add_lead(
            bar_start + step * BEAT / 2.0,
            note + octave,
            BEAT * (0.42 if phrase < 2 else 0.46),
            0.14 if phrase == 0 else 0.18,
            0.18,
        )

    for beat_index in range(4):
        beat_start = bar_start + beat_index * BEAT
        add_kick(beat_start, 1.08 if beat_index == 0 else 0.86)
        if beat_index in (1, 3):
            add_snare(beat_start, 0.92)
        add_shaker(beat_start, -0.35)
        add_shaker(beat_start + BEAT / 2.0, 0.35)

    if bar_index % 4 == 3:
        add_cowbell(bar_start + 3.5 * BEAT, 79, 0.9)


# A quiet circular slap delay adds space while preserving the loop boundary.
delay_frames = int(0.1875 * SAMPLE_RATE)
echo = np.roll(mix, delay_frames, axis=0)
mix += echo * 0.105

# Soft saturation and deterministic normalization keep headroom for sound effects.
mix = np.tanh(mix * 1.12)
peak = float(np.max(np.abs(mix)))
if peak > 0.0:
    mix *= 0.88 / peak

pcm = np.asarray(np.clip(mix, -1.0, 1.0) * 32767.0, dtype="<i2")
output = Path(__file__).resolve().parents[1] / "tmp" / "road_rooster_theme.wav"
output.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(output), "wb") as wav:
    wav.setnchannels(2)
    wav.setsampwidth(2)
    wav.setframerate(SAMPLE_RATE)
    wav.writeframes(pcm.tobytes())

print(f"Generated {output} ({DURATION:.1f}s, {BPM} BPM)")
