"""The upload flow end to end against `FakePartnerCenter` — no credentials."""

from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

from fake_partner_center import BASE, BLOB, MSIX_NAME, FakePartnerCenter, Response, reply
from store_http import StoreError, access_token
from submit_msix import zip_msix


class TokenAndZipTests(unittest.TestCase):
    def test_access_token_reads_bearer(self):
        self.assertEqual(access_token("tenant", "id", "secret", opener=reply(200, {"access_token": "tok-1"})), "tok-1")

    def test_access_token_missing_field(self):
        with self.assertRaises(StoreError):
            access_token("tenant", "id", "secret", opener=reply(200, {}))

    def test_zip_msix_stores_basename_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            msix = root / "nested" / MSIX_NAME
            msix.parent.mkdir()
            msix.write_bytes(b"msix-bytes")
            dest = root / "upload.zip"
            self.assertEqual(zip_msix(msix, dest), MSIX_NAME)
            with zipfile.ZipFile(dest) as archive:
                self.assertEqual(archive.namelist(), [MSIX_NAME])
                self.assertEqual(archive.read(MSIX_NAME), b"msix-bytes")


class SubmitFlowTests(unittest.TestCase):
    def test_updates_uploads_commits_then_waits_for_status(self):
        fake = FakePartnerCenter(statuses=("CommitStarted", "PreProcessing"))
        sleeps: list = []
        self.assertEqual(fake.run(sleeps), "sub-9")
        self.assertEqual(fake.put_body["flightPackages"][-1]["fileName"], MSIX_NAME)
        self.assertLess(fake.index("PUT", "/submissions/sub-9"), fake.index("PUT", BLOB))
        self.assertLess(fake.index("PUT", BLOB), fake.index("POST", "/commit"))
        self.assertLess(fake.index("POST", "/commit"), fake.index("GET", "/status"))
        self.assertEqual(len(sleeps), 1)
        self.assertFalse(any(m == "POST" and u.endswith("/flights") for m, u in fake.calls))

    def test_deletes_pending_before_creating(self):
        pending = {"friendlyName": "internal", "flightId": "f1", "pendingFlightSubmission": {"id": "old-sub"}}
        fake = FakePartnerCenter(flights={"value": [pending]})
        fake.run()
        self.assertLess(fake.index("DELETE", "/submissions/old-sub"), fake.index("POST", "/f1/submissions"))

    def test_missing_flight_fails_without_creating_one(self):
        fake = FakePartnerCenter(flights={"value": [{"friendlyName": "beta", "flightId": "b"}]})
        with self.assertRaises(StoreError) as caught:
            fake.run()
        self.assertTrue(caught.exception.bootstrap)
        self.assertIn("beta", str(caught.exception))
        self.assertEqual([m for m, _ in fake.calls], ["POST", "GET"])

    def test_no_flights_404_is_bootstrap(self):
        fake = FakePartnerCenter(flights=Response(404))
        with self.assertRaises(StoreError) as caught:
            fake.run()
        self.assertTrue(caught.exception.bootstrap)

    def test_follows_next_link(self):
        fake = FakePartnerCenter()
        fake.pages = {
            f"{BASE}/listflights": {
                "value": [{"friendlyName": "beta", "flightId": "b"}],
                "@nextLink": "applications/9NAPP/listflights/?skip=1&top=1",
            },
            f"{BASE}/listflights/?skip=1&top=1": {"value": [{"friendlyName": "internal", "flightId": "f1"}]},
        }
        self.assertEqual(fake.run(), "sub-9")
        self.assertEqual(fake.index("GET", "?skip=1&top=1"), 2)

    def test_commit_failed_raises_with_details(self):
        failed = {"status": "CommitFailed", "statusDetails": {"errors": [{"code": "MissingFiles", "details": "no msix"}]}}
        fake = FakePartnerCenter(statuses=("CommitStarted", failed))
        with self.assertRaises(StoreError) as caught:
            fake.run()
        self.assertIn("CommitFailed", str(caught.exception))
        self.assertIn("MissingFiles: no msix", str(caught.exception))

    def test_missing_upload_url_fails(self):
        fake = FakePartnerCenter()
        fake.submission = {"id": "sub-9", "flightPackages": []}
        with self.assertRaises(StoreError) as caught:
            fake.run()
        self.assertIn("fileUploadUrl", str(caught.exception))


if __name__ == "__main__":
    unittest.main()
