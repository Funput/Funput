"""Partner Center flight payloads and bootstrap detection — no credentials."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

from store_flight import (  # noqa: E402
    BOOTSTRAP_HINT,
    bootstrap_required,
    find_flight,
    new_flight_body,
    pending_submission_id,
    with_package,
)
from store_http import StoreError, access_token, api_json  # noqa: E402
from submit_msix import submit, zip_msix  # noqa: E402


class FlightLookupTests(unittest.TestCase):
    def test_finds_flight_by_exact_name(self):
        flights = [{"flightName": "beta", "flightId": "b"}, {"flightName": "internal", "flightId": "i"}]
        self.assertEqual(find_flight(flights, "internal")["flightId"], "i")

    def test_missing_flight_is_none(self):
        self.assertIsNone(find_flight([{"flightName": "beta"}], "internal"))

    def test_empty_list_is_none(self):
        self.assertIsNone(find_flight([], "internal"))

    def test_pending_id_present(self):
        flight = {"pendingFlightSubmission": {"id": "sub-1"}}
        self.assertEqual(pending_submission_id(flight), "sub-1")

    def test_pending_id_absent_or_empty(self):
        self.assertIsNone(pending_submission_id({}))
        self.assertIsNone(pending_submission_id({"pendingFlightSubmission": {}}))
        self.assertIsNone(pending_submission_id({"pendingFlightSubmission": {"id": ""}}))

    def test_new_flight_body_uses_empty_groups(self):
        self.assertEqual(new_flight_body("internal"), {"flightName": "internal", "groupIds": []})


class PackagePayloadTests(unittest.TestCase):
    def test_adds_pending_upload_and_marks_old_for_delete(self):
        submission = {
            "id": "s1",
            "applicationPackages": [
                {"fileName": "old.msix", "fileStatus": "Uploaded", "version": "1.0.0.0"}
            ],
        }
        updated = with_package(submission, "Funput-1.2026.66.101.msix")
        self.assertEqual(
            updated["applicationPackages"],
            [
                {"fileName": "old.msix", "fileStatus": "PendingDelete", "version": "1.0.0.0"},
                {"fileName": "Funput-1.2026.66.101.msix", "fileStatus": "PendingUpload"},
            ],
        )
        self.assertEqual(submission["applicationPackages"][0]["fileStatus"], "Uploaded")

    def test_empty_package_list_only_adds_upload(self):
        updated = with_package({}, "Funput.msix")
        self.assertEqual(
            updated["applicationPackages"],
            [{"fileName": "Funput.msix", "fileStatus": "PendingUpload"}],
        )

    def test_drops_in_flight_pending_upload_from_previous_edit(self):
        submission = {
            "applicationPackages": [{"fileName": "stale.msix", "fileStatus": "PendingUpload"}]
        }
        updated = with_package(submission, "next.msix")
        names = [pkg["fileName"] for pkg in updated["applicationPackages"]]
        self.assertEqual(names, ["next.msix"])


class BootstrapTests(unittest.TestCase):
    def test_age_rating_body_is_bootstrap(self):
        self.assertTrue(bootstrap_required(400, "Please complete the age ratings questionnaire."))
        self.assertTrue(BOOTSTRAP_HINT.startswith("Partner Center has no usable submission"))

    def test_first_submission_wording_is_bootstrap(self):
        self.assertTrue(
            bootstrap_required(409, "You must create one submission for the app in Partner Center.")
        )

    def test_unrelated_409_is_not_bootstrap(self):
        self.assertFalse(bootstrap_required(409, "A submission is already in progress."))

    def test_api_json_surfaces_bootstrap_hint(self):
        # Drive store_http.request through a urlopen-like opener (no network).
        class Response:
            def getcode(self):
                return 400

            def read(self):
                return b'{"code":"Invalid","message":"Complete the age rating first."}'

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        with self.assertRaises(StoreError) as caught:
            api_json("POST", "/applications/9NAPP/flights", "token", opener=lambda _req: Response())
        self.assertTrue(caught.exception.bootstrap)
        self.assertIn("age ratings", str(caught.exception))


class TokenAndZipTests(unittest.TestCase):
    def test_access_token_reads_bearer(self):
        class Response:
            def getcode(self):
                return 200

            def read(self):
                return b'{"access_token":"tok-1"}'

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        token = access_token("tenant", "id", "secret", opener=lambda _req: Response())
        self.assertEqual(token, "tok-1")

    def test_access_token_missing_field(self):
        class Response:
            def getcode(self):
                return 200

            def read(self):
                return b"{}"

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        with self.assertRaises(StoreError):
            access_token("tenant", "id", "secret", opener=lambda _req: Response())

    def test_zip_msix_stores_basename_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            msix = root / "nested" / "Funput-1.2026.66.101.msix"
            msix.parent.mkdir()
            msix.write_bytes(b"msix-bytes")
            dest = root / "upload.zip"
            name = zip_msix(msix, dest)
            self.assertEqual(name, "Funput-1.2026.66.101.msix")
            with zipfile.ZipFile(dest) as archive:
                self.assertEqual(archive.namelist(), [name])
                self.assertEqual(archive.read(name), b"msix-bytes")


class SubmitFlowTests(unittest.TestCase):
    def test_submit_creates_flight_uploads_and_commits(self):
        calls: list[tuple[str, str]] = []

        class Response:
            def __init__(self, status: int, payload: bytes):
                self._status = status
                self._payload = payload

            def getcode(self):
                return self._status

            def read(self):
                return self._payload

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        def opener(req):
            url = req.full_url
            calls.append((req.get_method(), url))
            if "login.microsoftonline.com" in url:
                return Response(200, b'{"access_token":"tok"}')
            if url.endswith("/flights") and req.get_method() == "GET":
                return Response(200, b'{"value":[]}')
            if url.endswith("/flights") and req.get_method() == "POST":
                return Response(200, b'{"flightId":"f1","flightName":"internal"}')
            if url.endswith("/submissions") and req.get_method() == "POST":
                return Response(
                    200,
                    json.dumps(
                        {
                            "id": "sub-9",
                            "fileUploadUrl": "https://blob.example/upload",
                            "applicationPackages": [],
                        }
                    ).encode(),
                )
            if url == "https://blob.example/upload":
                return Response(201, b"")
            if "/submissions/sub-9" in url and req.get_method() == "PUT":
                body = json.loads(req.data.decode())
                self.assertEqual(
                    body["applicationPackages"][-1],
                    {"fileName": "Funput-1.2026.66.101.msix", "fileStatus": "PendingUpload"},
                )
                return Response(200, b"{}")
            if url.endswith("/commit"):
                return Response(200, b"{}")
            raise AssertionError(f"unexpected {req.get_method()} {url}")

        with tempfile.TemporaryDirectory() as tmp:
            msix = Path(tmp) / "Funput-1.2026.66.101.msix"
            msix.write_bytes(b"pkg")
            env = {
                "MS_STORE_TENANT_ID": "t",
                "MS_STORE_CLIENT_ID": "c",
                "MS_STORE_CLIENT_SECRET": "s",
                "MS_STORE_APP_ID": "9NAPP",
                "MS_STORE_FLIGHT_NAME": "internal",
            }
            ident = submit(msix, env=env, opener=opener)
        self.assertEqual(ident, "sub-9")
        methods = [method for method, _ in calls]
        self.assertIn("POST", methods)
        self.assertTrue(any(url.endswith("/commit") for _, url in calls))

    def test_submit_deletes_pending_before_creating(self):
        seen = []

        class Response:
            def __init__(self, status, payload):
                self._status, self._payload = status, payload

            def getcode(self):
                return self._status

            def read(self):
                return self._payload

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        def opener(req):
            seen.append((req.get_method(), req.full_url))
            if "login.microsoftonline.com" in req.full_url:
                return Response(200, b'{"access_token":"tok"}')
            if req.full_url.endswith("/flights") and req.get_method() == "GET":
                return Response(
                    200,
                    json.dumps(
                        {
                            "value": [
                                {
                                    "flightName": "internal",
                                    "flightId": "f1",
                                    "pendingFlightSubmission": {"id": "old-sub"},
                                }
                            ]
                        }
                    ).encode(),
                )
            if req.get_method() == "DELETE":
                self.assertIn("/submissions/old-sub", req.full_url)
                return Response(200, b"")
            if req.full_url.endswith("/submissions") and req.get_method() == "POST":
                return Response(
                    200,
                    b'{"id":"new-sub","fileUploadUrl":"https://blob.example/u","applicationPackages":[]}',
                )
            if req.full_url == "https://blob.example/u":
                return Response(201, b"")
            return Response(200, b"{}")

        with tempfile.TemporaryDirectory() as tmp:
            msix = Path(tmp) / "Funput.msix"
            msix.write_bytes(b"pkg")
            submit(
                msix,
                env={
                    "MS_STORE_TENANT_ID": "t",
                    "MS_STORE_CLIENT_ID": "c",
                    "MS_STORE_CLIENT_SECRET": "s",
                    "MS_STORE_APP_ID": "9NAPP",
                    "MS_STORE_FLIGHT_NAME": "internal",
                },
                opener=opener,
            )
        self.assertTrue(any(m == "DELETE" and "old-sub" in u for m, u in seen))
        post_idx = next(i for i, (m, u) in enumerate(seen) if m == "POST" and u.endswith("/submissions"))
        delete_idx = next(i for i, (m, u) in enumerate(seen) if m == "DELETE")
        self.assertLess(delete_idx, post_idx)

    def test_missing_upload_url_fails(self):
        class Response:
            def __init__(self, payload):
                self._payload = payload

            def getcode(self):
                return 200

            def read(self):
                return self._payload

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        def opener(req):
            if "login.microsoftonline.com" in req.full_url:
                return Response(b'{"access_token":"tok"}')
            if req.full_url.endswith("/flights"):
                return Response(b'{"value":[{"flightName":"internal","flightId":"f1"}]}')
            return Response(b'{"id":"s","applicationPackages":[]}')

        with tempfile.TemporaryDirectory() as tmp:
            msix = Path(tmp) / "Funput.msix"
            msix.write_bytes(b"x")
            with self.assertRaises(StoreError) as caught:
                submit(
                    msix,
                    env={
                        "MS_STORE_TENANT_ID": "t",
                        "MS_STORE_CLIENT_ID": "c",
                        "MS_STORE_CLIENT_SECRET": "s",
                        "MS_STORE_APP_ID": "9NAPP",
                        "MS_STORE_FLIGHT_NAME": "internal",
                    },
                    opener=opener,
                )
        self.assertIn("fileUploadUrl", str(caught.exception))


if __name__ == "__main__":
    unittest.main()
