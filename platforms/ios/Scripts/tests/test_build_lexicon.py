"""Exercise packaging failures without Xcode or downloading dictionary data."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class BuildLexiconTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="lexicon build ")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.script = self.root / "platforms/ios/Scripts/build-lexicon.sh"
        self.script.parent.mkdir(parents=True)
        shutil.copyfile(Path(__file__).resolve().parents[1] / "build-lexicon.sh", self.script)
        self.tsv = self.root / "crates/funput-suggestions/data/lexicon/en.tsv"
        self.tsv.parent.mkdir(parents=True)
        self.tsv.write_bytes(b"first verified output")
        self.bin = self.root / ".cargo/bin"
        self.bin.mkdir(parents=True)
        self.tool("rustc", "echo 'host: test-host'\n")
        self.tool("cargo", 'cat\n')
        self.env = dict(os.environ, HOME=str(self.root),
                        DERIVED_FILE_DIR=str(self.root / "derived"),
                        TARGET_BUILD_DIR=str(self.root / "products"),
                        UNLOCALIZED_RESOURCES_FOLDER_PATH="Keyboard.appex")
        self.generated = self.root / "derived/Lexicon/en.lex"
        self.bundled = self.root / "products/Keyboard.appex/en.lex"

    def tool(self, name, body):
        path = self.bin / name
        path.write_text("#!/bin/sh\nset -eu\n" + body)
        path.chmod(0o755)

    def run_build(self):
        return subprocess.run(["sh", str(self.script)], env=self.env,
                              capture_output=True, text=True)

    def test_clean_and_incremental_build_use_current_input(self):
        self.tool("cargo", 'printf "%s\\n" "$@" > "$HOME/arguments"\ncat\n')
        self.assertEqual(self.run_build().returncode, 0)
        self.assertEqual(self.bundled.read_bytes(), self.tsv.read_bytes())
        args = (self.root / "arguments").read_text().splitlines()
        self.assertIn("--locked", args)
        self.assertIn("--release", args)
        self.assertIn("test-host", args)
        self.assertEqual(args[-2:], ["--", "pack"])
        self.tsv.write_bytes(b"updated verified output")
        self.assertEqual(self.run_build().returncode, 0)
        self.assertEqual(self.bundled.read_bytes(), self.tsv.read_bytes())
        self.assertEqual(self.generated.read_bytes(), self.tsv.read_bytes())
        self.assertEqual(self.bundled.stat().st_mode & 0o777, 0o644)

    def test_failed_pack_keeps_previous_artifacts(self):
        self.assertEqual(self.run_build().returncode, 0)
        self.tool("cargo", 'printf broken\nexit 9\n')
        self.assertNotEqual(self.run_build().returncode, 0)
        self.assertEqual(self.bundled.read_bytes(), self.tsv.read_bytes())
        self.assertEqual(self.generated.read_bytes(), self.tsv.read_bytes())
        self.assertEqual(list(self.generated.parent.glob("en.lex.*")), [])

    def test_empty_or_oversized_output_fails_without_resource(self):
        for data in [b"", b"x" * 524289]:
            with self.subTest(size=len(data)):
                self.tsv.write_bytes(data)
                self.assertNotEqual(self.run_build().returncode, 0)
                self.assertFalse(self.bundled.exists())
                self.assertFalse(self.generated.exists())


if __name__ == "__main__":
    unittest.main()
