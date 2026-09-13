"""Partner Center flight payloads, status and bootstrap detection — pure, no network."""

from __future__ import annotations

import unittest

from fake_partner_center import reply  # first: it also puts scripts/store on sys.path
from store_flight import (
    BOOTSTRAP_HINT,
    as_flight_list,
    bootstrap_required,
    commit_failed,
    find_flight,
    missing_flight_message,
    pending_submission_id,
    status_errors,
    with_package,
)
from store_http import StoreError, api_json


class FlightLookupTests(unittest.TestCase):
    def test_finds_listed_flight_by_friendly_name(self):
        flights = [{"friendlyName": "beta", "flightId": "b"}, {"friendlyName": "internal", "flightId": "i"}]
        self.assertEqual(find_flight(flights, "internal")["flightId"], "i")

    def test_falls_back_to_flight_name(self):
        self.assertEqual(find_flight([{"flightName": "internal", "flightId": "i"}], "internal")["flightId"], "i")

    def test_missing_flight_is_none(self):
        self.assertIsNone(find_flight([{"friendlyName": "beta"}], "internal"))
        self.assertIsNone(find_flight([], "internal"))

    def test_as_flight_list_accepts_value_wrapper_and_bare_array(self):
        flight = {"friendlyName": "internal", "flightId": "i"}
        self.assertEqual(as_flight_list({"value": [flight], "totalCount": 1}), [flight])
        self.assertEqual(as_flight_list([flight]), [flight])
        self.assertEqual(as_flight_list("nope"), [])
        self.assertEqual(as_flight_list({"value": "x"}), [])

    def test_pending_id_present_absent_or_empty(self):
        self.assertEqual(pending_submission_id({"pendingFlightSubmission": {"id": "sub-1"}}), "sub-1")
        self.assertIsNone(pending_submission_id({}))
        self.assertIsNone(pending_submission_id({"pendingFlightSubmission": {"id": ""}}))

    def test_missing_flight_message_names_known_flights_and_bootstrap(self):
        message = missing_flight_message("internal", [{"friendlyName": "beta"}])
        self.assertIn("'internal'", message)
        self.assertIn("beta", message)
        self.assertIn(BOOTSTRAP_HINT, message)
        self.assertIn("none", missing_flight_message("internal", []))


class PackagePayloadTests(unittest.TestCase):
    NEW = {
        "fileName": "Funput-1.2026.66.0.msix",
        "fileStatus": "PendingUpload",
        "minimumDirectXVersion": "None",
        "minimumSystemRam": "None",
    }

    def test_adds_pending_upload_and_marks_old_for_delete(self):
        submission = {
            "id": "s1",
            "flightPackages": [{"fileName": "old.msix", "fileStatus": "Uploaded", "version": "1.0.0.0"}],
        }
        updated = with_package(submission, "Funput-1.2026.66.0.msix")
        self.assertEqual(
            updated["flightPackages"],
            [{"fileName": "old.msix", "fileStatus": "PendingDelete", "version": "1.0.0.0"}, self.NEW],
        )
        self.assertEqual(submission["flightPackages"][0]["fileStatus"], "Uploaded")

    def test_writes_flight_packages_not_application_packages(self):
        updated = with_package({"id": "s1"}, "Funput-1.2026.66.0.msix")
        self.assertEqual(updated["flightPackages"], [self.NEW])
        self.assertNotIn("applicationPackages", updated)

    def test_drops_in_flight_pending_upload_from_previous_edit(self):
        submission = {"flightPackages": [{"fileName": "stale.msix", "fileStatus": "PendingUpload"}]}
        names = [pkg["fileName"] for pkg in with_package(submission, "next.msix")["flightPackages"]]
        self.assertEqual(names, ["next.msix"])


class StatusTests(unittest.TestCase):
    def test_failed_statuses(self):
        for status in ("CommitFailed", "PreProcessingFailed", "CertificationFailed", "Canceled"):
            self.assertTrue(commit_failed(status), status)
        for status in ("CommitStarted", "PreProcessing", "Certification", "PendingCommit"):
            self.assertFalse(commit_failed(status), status)

    def test_status_errors_lists_code_and_details(self):
        payload = {"statusDetails": {"errors": [{"code": "InvalidArchive", "details": "bad zip"}]}}
        self.assertEqual(status_errors(payload), "InvalidArchive: bad zip")
        self.assertIn("no error details", status_errors({}))


class BootstrapTests(unittest.TestCase):
    def test_age_rating_body_is_bootstrap(self):
        self.assertTrue(bootstrap_required(400, "Please complete the age ratings questionnaire."))
        self.assertIn("published", BOOTSTRAP_HINT)

    def test_first_submission_wording_is_bootstrap(self):
        self.assertTrue(bootstrap_required(409, "You must create one submission for the app in Partner Center."))

    def test_unrelated_409_is_not_bootstrap(self):
        self.assertFalse(bootstrap_required(409, "A submission is already in progress."))

    def test_api_json_surfaces_bootstrap_hint(self):
        body = {"code": "Invalid", "message": "Complete the age rating first."}
        with self.assertRaises(StoreError) as caught:
            api_json("GET", "/applications/9NAPP/listflights", "token", opener=reply(400, body))
        self.assertTrue(caught.exception.bootstrap)
        self.assertIn("published", str(caught.exception))

    def test_api_json_404_is_none_only_when_missing_ok(self):
        self.assertIsNone(api_json("GET", "/x", "token", missing_ok=True, opener=reply(404)))
        with self.assertRaises(StoreError):
            api_json("GET", "/x", "token", opener=reply(404))


if __name__ == "__main__":
    unittest.main()
