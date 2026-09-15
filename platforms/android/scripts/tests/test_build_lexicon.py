"""Exercise publication failures with a fake host packer; no network or Cargo cache."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "build-lexicon.sh"


class PackagingTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        data = self.root / "crates/funput-suggestions/data/lexicon"
        data.mkdir(parents=True)
        (data / "en.tsv").write_text("example\t1\n")
        (data / "NOTICE.md").write_text("Original notice\n")
        self.output = self.root / "output"
        self.bin = self.root / "bin"
        self.bin.mkdir()
        rustc = self.bin / "rustc"
        rustc.write_text("#!/bin/sh\necho 'host: test-host'\n")
        rustc.chmod(0o755)

    def run_pack(self, body):
        cargo = self.bin / "cargo"
        cargo.write_text("#!/bin/sh\n" + body)
        cargo.chmod(0o755)
        return subprocess.run(["bash", str(SCRIPT), str(self.root), str(self.output)],
                              env={**os.environ, "PATH": str(self.bin) + ":" + os.environ["PATH"]},
                              capture_output=True)

    def test_failure_keeps_previous_artifact_and_removes_temporary_files(self):
        for body in ("printf partial; exit 1\n", "printf short\n", "head -c 524289 /dev/zero\n"):
            with self.subTest(body=body):
                self.assertEqual(0, self.run_pack("head -c 24 /dev/zero\n").returncode)
                lexicon = self.output / "lexicon/en.lex"
                original = lexicon.read_bytes()
                self.assertNotEqual(0, self.run_pack(body).returncode)
                self.assertEqual(original, lexicon.read_bytes())
                self.assertEqual([lexicon], list(lexicon.parent.glob("en.lex*")))
                self.assertEqual("Original notice\n", (lexicon.parent / "NOTICE.md").read_text())

    def test_clean_failure_publishes_nothing(self):
        self.assertNotEqual(0, self.run_pack("printf partial; exit 1\n").returncode)
        self.assertEqual([], list((self.output / "lexicon").iterdir()))


if __name__ == "__main__":
    unittest.main()
