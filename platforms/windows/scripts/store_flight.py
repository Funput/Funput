"""Pure Partner Center flight helpers — no network, no secrets.

`submit_msix.py` calls these with JSON the API already returned. Tests cover
version-adjacent payload shape and the first-submission bootstrap failure
without an Azure AD app.
"""

from __future__ import annotations

from typing import Any

BOOTSTRAP_HINT = (
    "Partner Center has no usable submission yet. In the portal: finish the "
    "store listing, answer the age ratings questionnaire, and create one "
    "submission by hand. The Store API cannot create that first submission. "
    "Then create (or let CI create) the package flight named in "
    "MS_STORE_FLIGHT_NAME."
)

_BOOTSTRAP_MARKERS = (
    "age rating",
    "agerating",
    "questionnaire",
    "first submission",
    "create one submission",
    "create a submission for the app in partner center",
)


def find_flight(flights: list[dict[str, Any]], name: str) -> dict[str, Any] | None:
    for flight in flights:
        if flight.get("flightName") == name:
            return flight
    return None


def pending_submission_id(flight: dict[str, Any]) -> str | None:
    pending = flight.get("pendingFlightSubmission") or {}
    ident = pending.get("id")
    return ident if ident else None


def bootstrap_required(status: int, body: str) -> bool:
    lower = body.lower()
    if any(marker in lower for marker in _BOOTSTRAP_MARKERS):
        return True
    return status in (400, 409) and "age" in lower and "submission" in lower


def new_flight_body(name: str) -> dict[str, Any]:
    return {"flightName": name, "groupIds": []}


def with_package(submission: dict[str, Any], file_name: str) -> dict[str, Any]:
    """Replace current packages with `file_name` (the .msix inside the upload zip)."""
    updated = dict(submission)
    outgoing: list[dict[str, Any]] = []
    for package in submission.get("applicationPackages") or []:
        entry = dict(package)
        if entry.get("fileStatus") != "PendingUpload":
            entry["fileStatus"] = "PendingDelete"
            outgoing.append(entry)
    outgoing.append({"fileName": file_name, "fileStatus": "PendingUpload"})
    updated["applicationPackages"] = outgoing
    return updated
