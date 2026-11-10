#!/usr/bin/env python3
"""Synthesize Funput's bundled key sounds.

Every sound is generated from code here, so the files carry the repository's MIT
licence and can be rebuilt byte for byte. Each style has three sounds that mirror
the system keyboard's split: a key, delete, and a modifier (space, shift, return).
The iOS and Android keyboards bundle the same sounds.
"""

import argparse
import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44_100
PEAK = 0.89  # About -1 dBFS, leaving headroom for resampling on device.
FADE_IN_SECONDS = 0.0005
FADE_OUT_SECONDS = 0.006
KINDS = ("key", "delete", "modifier")
REPO_ROOT = Path(__file__).resolve().parents[1]
# Android resource names allow only lowercase letters, digits and underscores.
TARGETS = (
    ("platforms/ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/KeySounds", "{style}-{kind}.wav"),
    ("platforms/android/keyboard-renderer/src/main/res/raw", "key_sound_{style}_{kind}.wav"),
)

# Delete sits lower and the modifier slightly lower and longer, as on the stock keyboard.
PITCH = {"key": 1.0, "delete": 0.78, "modifier": 0.88}
LENGTH = {"key": 1.0, "delete": 1.1, "modifier": 1.2}
# Marimba plays real notes instead: A5, E5 and C#5 spell an A major chord.
MARIMBA_NOTES = {"key": 880.0, "delete": 554.37, "modifier": 659.26}
# The typebar falls back against its rest a beat after it strikes.
TYPEBAR_REBOUND_SECONDS = 0.018


def damped_sine(t: float, frequency: float, decay: float, phase: float = 0.0) -> float:
    return math.sin(2 * math.pi * frequency * t + phase) * math.exp(-t / decay)


def mechanical(t: float, kind: str, noise: float) -> float:
    pitch = PITCH[kind]
    click = noise * math.exp(-t / 0.0025)
    ping = 0.45 * damped_sine(t, 2_900 * pitch, 0.008)
    thock = 0.7 * damped_sine(t, 170 * pitch, 0.022)
    return click + ping + thock


def wood(t: float, kind: str, noise: float) -> float:
    pitch = PITCH[kind]
    knock = 0.25 * noise * math.exp(-t / 0.0015)
    body = damped_sine(t, 820 * pitch, 0.014) + 0.55 * damped_sine(t, 1_730 * pitch, 0.007)
    return knock + body


def bubble(t: float, kind: str, noise: float) -> float:
    # An upward chirp: the integral of a frequency that rises exponentially.
    start, rise = 380 * PITCH[kind], 18.0
    phase = 2 * math.pi * start * (math.exp(rise * t) - 1) / rise
    return math.sin(phase) * math.exp(-t / 0.018) * (1 - math.exp(-t / 0.002))


def typewriter(t: float, kind: str, noise: float) -> float:
    pitch = PITCH[kind]
    strike = noise * math.exp(-t / 0.0015)
    metal = 0.35 * (damped_sine(t, 3_400 * pitch, 0.006) + damped_sine(t, 5_300 * pitch, 0.004))
    body = 0.6 * damped_sine(t, 210 * pitch, 0.014)
    since_rebound = t - TYPEBAR_REBOUND_SECONDS
    rebound = 0.45 * noise * math.exp(-since_rebound / 0.002) if since_rebound >= 0 else 0.0
    return strike + metal + body + rebound


def marimba(t: float, kind: str, noise: float) -> float:
    note = MARIMBA_NOTES[kind]
    mallet = 0.15 * noise * math.exp(-t / 0.001)
    # A marimba bar's strongest overtone sits two octaves up, with a faint tenth partial.
    tone = (damped_sine(t, note, 0.028) + 0.35 * damped_sine(t, 4 * note, 0.006)
            + 0.08 * damped_sine(t, 10 * note, 0.002))
    return mallet + tone * (1 - math.exp(-t / 0.0008))


STYLES = {
    "mechanical": (mechanical, 0.045),
    "wood": (wood, 0.050),
    "bubble": (bubble, 0.075),
    "typewriter": (typewriter, 0.060),
    "marimba": (marimba, 0.070),
}


def render(style: str, kind: str) -> list[int]:
    voice, base_seconds = STYLES[style]
    seconds = base_seconds * LENGTH[kind]
    count = round(seconds * SAMPLE_RATE)
    rng = random.Random(f"{style}-{kind}")
    smoothed = 0.0
    raw: list[float] = []
    for index in range(count):
        t = index / SAMPLE_RATE
        # A one-pole low-pass keeps the noise from hissing.
        smoothed += 0.35 * (rng.uniform(-1, 1) - smoothed)
        raw.append(voice(t, kind, smoothed))
    return shape(raw)


def shape(raw: list[float]) -> list[int]:
    loudest = max(abs(value) for value in raw) or 1.0
    fade_in = max(1, round(FADE_IN_SECONDS * SAMPLE_RATE))
    fade_out = max(1, round(FADE_OUT_SECONDS * SAMPLE_RATE))
    samples = []
    for index, value in enumerate(raw):
        gain = min(1.0, index / fade_in, (len(raw) - 1 - index) / fade_out)
        samples.append(round(value / loudest * PEAK * gain * 32_767))
    return samples


def write(path: Path, samples: list[int]) -> None:
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack(f"<{len(samples)}h", *samples))


def generate(root: Path) -> list[Path]:
    sounds = {(style, kind): render(style, kind) for style in STYLES for kind in KINDS}
    paths = []
    for directory, name in TARGETS:
        output = root / directory
        output.mkdir(parents=True, exist_ok=True)
        for (style, kind), samples in sounds.items():
            path = output / name.format(style=style, kind=kind)
            write(path, samples)
            paths.append(path)
    return paths


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=REPO_ROOT, help="repository root to write under")
    arguments = parser.parse_args()
    for path in generate(arguments.root):
        print(path)


if __name__ == "__main__":
    main()
