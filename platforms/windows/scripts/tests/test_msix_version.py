"""MSIX Identity.Version stamping — no Windows SDK, no Partner Center."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

from msix_version import VersionError, main, package_version, parse_u16  # noqa: E402

MANIFEST = SCRIPTS.parent / "msix" / "AppxManifest.xml.template"
STARTUP = SCRIPTS.parent / "src" / "shared" / "startup_task.rs"


class PackageVersionTests(unittest.TestCase):
    def test_three_part_marketing_plus_revision(self):
        self.assertEqual(package_version("1.2026.66", 101), "1.2026.66.101")

    def test_strips_surrounding_whitespace(self):
        self.assertEqual(package_version("  1.2026.1\n", 100), "1.2026.1.100")

    def test_zero_revision_is_legal_msix(self):
        self.assertEqual(package_version("1.0.0", 0), "1.0.0.0")

    def test_max_uint16_revision(self):
        self.assertEqual(package_version("1.2026.66", 65535), "1.2026.66.65535")

    def test_rejects_two_part_marketing(self):
        with self.assertRaises(VersionError) as caught:
            package_version("1.2026", 101)
        self.assertIn("three numeric parts", str(caught.exception))

    def test_rejects_four_part_marketing(self):
        with self.assertRaises(VersionError):
            package_version("1.2026.66.1", 101)

    def test_rejects_non_numeric_part(self):
        with self.assertRaises(VersionError) as caught:
            package_version("1.2026.x", 101)
        self.assertIn("build", str(caught.exception))

    def test_rejects_year_above_uint16(self):
        with self.assertRaises(VersionError) as caught:
            package_version("1.70000.1", 1)
        self.assertIn("65535", str(caught.exception))

    def test_rejects_revision_outside_uint16(self):
        with self.assertRaises(VersionError):
            package_version("1.2026.66", -1)
        with self.assertRaises(VersionError):
            package_version("1.2026.66", 65536)

    def test_parse_u16_rejects_leading_junk(self):
        with self.assertRaises(VersionError):
            parse_u16("01a", "minor")

    def test_cli_prints_version_and_exits_zero(self):
        from io import StringIO
        from unittest.mock import patch

        with patch("sys.stdout", new=StringIO()) as out:
            self.assertEqual(main(["1.2026.66", "101"]), 0)
        self.assertEqual(out.getvalue().strip(), "1.2026.66.101")

    def test_manifest_declares_full_trust_x64_and_startup_task(self):
        text = MANIFEST.read_text(encoding="utf-8")
        for token in (
            "__IDENTITY_NAME__",
            "__PUBLISHER__",
            "__PUBLISHER_DISPLAY_NAME__",
            "__VERSION__",
            'ProcessorArchitecture="x64"',
            'Name="runFullTrust"',
            'TaskId="FunputStartup"',
            'MinVersion="10.0.17763.0"',
        ):
            self.assertIn(token, text)
        rust = STARTUP.read_text(encoding="utf-8")
        self.assertIn('TASK_ID: &str = "FunputStartup"', rust)

    def test_cli_rejects_bad_marketing(self):
        from io import StringIO
        from unittest.mock import patch

        with patch("sys.stderr", new=StringIO()) as err:
            self.assertEqual(main(["1.2", "1"]), 1)
        self.assertIn("error:", err.getvalue())


if __name__ == "__main__":
    unittest.main()
