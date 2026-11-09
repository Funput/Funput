"""Keep the bundled key sounds reproducible from the generator script."""
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import wave

SCRIPTS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("generate_key_sounds", SCRIPTS / "generate-key-sounds.py")
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


def read_samples(path):
    with wave.open(str(path), "rb") as sound:
        frames = sound.getnframes()
        return struct.unpack(f"<{frames}h", sound.readframes(frames))


class GenerateKeySoundsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="key sounds ")
        self.addCleanup(self.temp.cleanup)
        self.paths = generator.generate(Path(self.temp.name))

    def test_one_file_per_style_and_kind(self):
        names = sorted(path.name for path in self.paths)
        styles = ("mechanical", "wood", "bubble", "typewriter", "marimba")
        expected = sorted(f"{style}-{kind}.wav" for style in styles
                          for kind in ("key", "delete", "modifier"))
        self.assertEqual(names, expected)

    def test_committed_files_match_the_generator(self):
        for path in self.paths:
            committed = generator.DEFAULT_OUTPUT / path.name
            self.assertTrue(committed.exists(), f"{committed} is missing; run generate-key-sounds.py")
            fresh, stored = read_samples(path), read_samples(committed)
            self.assertEqual(len(fresh), len(stored), f"{path.name} is stale")
            # Another platform's libm may round the last bit differently.
            drift = max(abs(a - b) for a, b in zip(fresh, stored))
            self.assertLessEqual(drift, 1, f"{path.name} is stale; run generate-key-sounds.py")

    def test_rerun_is_byte_identical(self):
        with tempfile.TemporaryDirectory(prefix="key sounds again ") as again:
            for path, other in zip(self.paths, generator.generate(Path(again))):
                self.assertEqual(path.read_bytes(), other.read_bytes())

    def test_format_length_and_level(self):
        for path in self.paths:
            with wave.open(str(path), "rb") as sound:
                self.assertEqual(sound.getnchannels(), 1)
                self.assertEqual(sound.getsampwidth(), 2)
                self.assertEqual(sound.getframerate(), generator.SAMPLE_RATE)
            samples = read_samples(path)
            seconds = len(samples) / generator.SAMPLE_RATE
            self.assertGreaterEqual(seconds, 0.04, path.name)
            self.assertLessEqual(seconds, 0.09, path.name)
            self.assertLess(max(abs(value) for value in samples), 32_767, path.name)
            self.assertEqual(samples[-1], 0, f"{path.name} ends with a click")


if __name__ == "__main__":
    unittest.main()
