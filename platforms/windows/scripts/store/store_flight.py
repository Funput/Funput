"""Pure Partner Center flight helpers — no network, no secrets.

`submit_msix.py` calls these with JSON the API already returned. Tests cover
the flight submission payload shape, commit status, and the bootstrap failure
without an Azure AD app.
"""

from __future__ import annotations

from typing import Any

BOOTSTRAP_HINT = (
    "Partner Center has no usable package flight yet. Package flights only "
    "exist after the app's first submission is *published*: finish the store "
    "listing and age ratings, submit by hand, and wait for it to pass "
    "certification. Then create a known user group and a package flight named "
    "as MS_STORE_FLIGHT_NAME in the portal — the API cannot do either for you."
)

_BOOTSTRAP_MARKERS = (
    "age rating",
    "agerating",
    "questionnaire",
    "first submission",
    "create one submission",
    "create a submission for the app in partner center",
)

# The commit is accepted while the status is CommitStarted; it then moves to
# PreProcessing on success or CommitFailed on error. Anything ending in
# "Failed" (or Canceled) later on is also a failure worth surfacing.
COMMIT_IN_PROGRESS = "CommitStarted"


def as_flight_list(payload: Any) -> list[dict[str, Any]]:
    """`listflights` answers `{value: [...]}`; tolerate a bare array too."""
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        raw = payload.get("value") or []
        return [item for item in raw if isinstance(item, dict)] if isinstance(raw, list) else []
    return []


def flight_name(flight: dict[str, Any]) -> str | None:
    """`listflights` calls it `friendlyName`; create-flight calls it `flightName`."""
    return flight.get("friendlyName") or flight.get("flightName")


def find_flight(flights: list[dict[str, Any]], name: str) -> dict[str, Any] | None:
    for flight in flights:
        if flight_name(flight) == name:
            return flight
    return None


def missing_flight_message(name: str, flights: list[dict[str, Any]]) -> str:
    known = ", ".join(sorted(str(flight_name(f)) for f in flights)) or "none"
    return f"No package flight named {name!r} (flights on this app: {known}).\n{BOOTSTRAP_HINT}"


def pending_submission_id(flight: dict[str, Any]) -> str | None:
    pending = flight.get("pendingFlightSubmission") or {}
    ident = pending.get("id")
    return ident if ident else None


def bootstrap_required(status: int, body: str) -> bool:
    lower = body.lower()
    if any(marker in lower for marker in _BOOTSTRAP_MARKERS):
        return True
    return status in (400, 409) and "age" in lower and "submission" in lower


def with_package(submission: dict[str, Any], file_name: str) -> dict[str, Any]:
    """Replace current flight packages with `file_name` (the .msix inside the upload zip)."""
    updated = dict(submission)
    outgoing: list[dict[str, Any]] = []
    for package in submission.get("flightPackages") or []:
        entry = dict(package)
        if entry.get("fileStatus") != "PendingUpload":
            entry["fileStatus"] = "PendingDelete"
            outgoing.append(entry)
    outgoing.append(
        {
            "fileName": file_name,
            "fileStatus": "PendingUpload",
            "minimumDirectXVersion": "None",
            "minimumSystemRam": "None",
        }
    )
    updated["flightPackages"] = outgoing
    return updated


def commit_failed(status: str) -> bool:
    return status.endswith("Failed") or status == "Canceled"


def status_errors(payload: dict[str, Any]) -> str:
    """`statusDetails.errors` as one line per `code: details`."""
    details = payload.get("statusDetails") or {}
    lines = [
        f"{error.get('code', 'Other')}: {error.get('details', '')}"
        for error in details.get("errors") or []
        if isinstance(error, dict)
    ]
    return "\n".join(lines) or "(Partner Center gave no error details)"
