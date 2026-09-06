"""Exercise release command construction without Apple credentials or a real build."""
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]


class ReleaseSigningTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="funput-signing-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        scripts = self.root / "Scripts"
        scripts.mkdir()
        shutil.copy2(SCRIPTS / "release-ios.sh", scripts)
        self.executable(scripts / "build-ffi.sh", "#!/bin/sh\nexit 0\n")
        self.executable(self.root / "xcodebuild", f"#!{sys.executable}\n" + '''
import json, os, sys
with open(os.environ["SIGNING_TEST_LOG"], "a") as log:
    log.write(json.dumps(sys.argv[1:]) + "\\n")
# Stop at export, after recording its arguments and the generated export plist.
# Signing and entitlement verification require real Apple assets and are not mocked.
sys.exit(73 if "-exportArchive" in sys.argv else 0)
''')
        self.key = self.root / "AuthKey_TEST.p8"
        self.key.write_text("test placeholder")
        self.env = dict(os.environ, CI="false", PROFILE_APP="", PROFILE_KEYBOARD="",
                        ASC_KEY_ID="TEST", ASC_ISSUER_ID="test-issuer", ASC_KEY_PATH=str(self.key),
                        SIGN_IDENTITY="Apple Distribution", TEAM_ID="TESTTEAM",
                        APP_BUNDLE_ID="app.funput.funput", APP_GROUP="group.app.funput.funput",
                        KEYBOARD_BUNDLE_ID="app.funput.funput.Keyboard", VERSION="1.2026.80", BUILD="123",
                        SIGNING_TEST_LOG=str(self.root / "calls.jsonl"),
                        PATH=str(self.root) + os.pathsep + os.environ["PATH"])

    def executable(self, path, source):
        path.write_text(source)
        path.chmod(0o755)

    def run_release(self, **env):
        result = subprocess.run(["sh", str(self.root / "Scripts/release-ios.sh")],
                                env=dict(self.env, **env), text=True, capture_output=True)
        log = self.root / "calls.jsonl"
        calls = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
        return result, calls

    def test_manual_archive_and_export_use_installed_assets(self):
        result, calls = self.run_release(CI="true", PROFILE_APP="App Store Profile",
                                         PROFILE_KEYBOARD="Keyboard Store Profile")
        self.assertEqual(result.returncode, 73, result.stderr)
        self.assertEqual(len(calls), 2)
        archive, export = calls
        for call in calls:
            self.assertNotIn("-allowProvisioningUpdates", call)
            self.assertNotIn("-authenticationKeyPath", call)
        for setting in ["CODE_SIGN_STYLE=Manual", "CODE_SIGN_IDENTITY=Apple Distribution",
                        "DEVELOPMENT_TEAM=TESTTEAM", "FUNPUT_APP_PROFILE=App Store Profile",
                        "FUNPUT_KEYBOARD_PROFILE=Keyboard Store Profile",
                        "MARKETING_VERSION=1.2026.80", "CURRENT_PROJECT_VERSION=123"]:
            self.assertIn(setting, archive)
        self.assertFalse(any(arg.startswith("PROVISIONING_PROFILE") for arg in archive))
        self.assertNotIn("CODE_SIGNING_ALLOWED=NO", archive)
        options = plistlib.loads(Path(export[export.index("-exportOptionsPlist") + 1]).read_bytes())
        self.assertEqual(options["signingStyle"], "manual")
        self.assertEqual(options["signingCertificate"], "Apple Distribution")
        self.assertEqual(options["provisioningProfiles"], {
            "app.funput.funput": "App Store Profile",
            "app.funput.funput.Keyboard": "Keyboard Store Profile",
        })

    def test_local_automatic_signing_retains_authentication(self):
        result, calls = self.run_release()
        self.assertEqual(result.returncode, 73, result.stderr)
        self.assertEqual(len(calls), 2)
        for call in calls:
            self.assertIn("-allowProvisioningUpdates", call)
            self.assertIn("-authenticationKeyPath", call)
        self.assertNotIn("CODE_SIGN_STYLE=Manual", calls[0])

    def test_missing_profiles_fail_before_building_on_ci(self):
        for profiles in [{}, {"PROFILE_APP": "app"}, {"PROFILE_KEYBOARD": "keyboard"}]:
            with self.subTest(profiles=profiles):
                result, calls = self.run_release(CI="true", **profiles)
                self.assertEqual(result.returncode, 1)
                self.assertIn("PROFILE_APP and PROFILE_KEYBOARD", result.stderr)
                self.assertEqual(calls, [])
                self.assertFalse((self.root / "build").exists())

    def test_partial_profiles_also_fail_locally(self):
        result, calls = self.run_release(PROFILE_APP="app")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(calls, [])

    def test_project_maps_each_app_target_to_its_own_profile(self):
        project = (SCRIPTS.parent / "Funput.xcodeproj/project.pbxproj").read_text()
        blocks = re.findall(r"buildSettings = \{(.*?)\n\s*\};", project, re.S)
        for target, variable in [("Funput", "FUNPUT_APP_PROFILE"), ("Keyboard", "FUNPUT_KEYBOARD_PROFILE")]:
            configs = [block for block in blocks if f"{target}/{target}.entitlements" in block]
            self.assertEqual(len(configs), 2)  # Debug and Release
            for config in configs:
                self.assertIn(f'PROVISIONING_PROFILE_SPECIFIER = "$({variable})";', config)
        self.assertEqual(project.count("PROVISIONING_PROFILE_SPECIFIER ="), 4)


if __name__ == "__main__":
    unittest.main()
