#!/usr/bin/env python3
"""Upload an .msix to a Partner Center package flight and commit it.

Uploading is not publishing to production. The flight's testers see the build;
promoting it still happens in Partner Center. The flight itself must already
exist (see `store_flight.BOOTSTRAP_HINT`) — the script never creates one.

Required environment: MS_STORE_TENANT_ID, MS_STORE_CLIENT_ID,
MS_STORE_CLIENT_SECRET, MS_STORE_APP_ID, MS_STORE_FLIGHT_NAME.
"""

from __future__ import annotations

import argparse
import os
import sys
import time
import zipfile
from pathlib import Path

from store_flight import (
    COMMIT_IN_PROGRESS,
    as_flight_list,
    commit_failed,
    find_flight,
    missing_flight_message,
    pending_submission_id,
    status_errors,
    with_package,
)
from store_http import StoreError, access_token, api_json, request

POLL_SECONDS = 15
# Commit usually settles in a minute or two; well inside the token's 60 minutes.
COMMIT_TIMEOUT_SECONDS = 20 * 60


def zip_msix(msix: Path, dest: Path) -> str:
    name = msix.name
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(msix, arcname=name)
    return name


def list_flights(app_id: str, token: str, **kwargs) -> list[dict]:
    """Every page of `listflights`; a 404 there means the app has no flights."""
    flights: list[dict] = []
    path: str | None = f"/applications/{app_id}/listflights"
    while path:
        page = api_json("GET", path, token, missing_ok=True, **kwargs)
        if page is None:
            break
        flights.extend(as_flight_list(page))
        next_link = page.get("@nextLink") if isinstance(page, dict) else None
        path = f"/{next_link.lstrip('/')}" if next_link else None
    return flights


def wait_for_commit(status_path: str, token: str, *, sleep=time.sleep, clock=time.monotonic, **kwargs) -> str:
    """Poll until the commit leaves CommitStarted; raise on a failed status."""
    deadline = clock() + COMMIT_TIMEOUT_SECONDS
    while True:
        payload = api_json("GET", status_path, token, **kwargs) or {}
        status = str(payload.get("status", ""))
        print(f"Submission status: {status}")
        if commit_failed(status):
            raise StoreError(f"Partner Center reports {status}:\n{status_errors(payload)}")
        if status != COMMIT_IN_PROGRESS:
            return status
        if clock() >= deadline:
            raise StoreError(f"Commit still {status} after {COMMIT_TIMEOUT_SECONDS // 60} minutes; check Partner Center.")
        sleep(POLL_SECONDS)


def submit(msix: Path, *, env: dict[str, str], opener=None, sleep=time.sleep) -> str:
    kwargs = {} if opener is None else {"opener": opener}
    token = access_token(env["MS_STORE_TENANT_ID"], env["MS_STORE_CLIENT_ID"], env["MS_STORE_CLIENT_SECRET"], **kwargs)
    app_id, name = env["MS_STORE_APP_ID"], env["MS_STORE_FLIGHT_NAME"]
    flights = list_flights(app_id, token, **kwargs)
    flight = find_flight(flights, name)
    if flight is None:
        raise StoreError(missing_flight_message(name, flights), bootstrap=True)
    base = f"/applications/{app_id}/flights/{flight['flightId']}/submissions"
    pending = pending_submission_id(flight)
    if pending:
        # Only one submission can be in progress per flight. This discards it —
        # including one someone is editing in the portal — so say so in the log.
        print(f"Deleting pending flight submission {pending} to make room for this upload.")
        api_json("DELETE", f"{base}/{pending}", token, **kwargs)
    submission = api_json("POST", base, token, **kwargs)
    upload = submission.get("fileUploadUrl")
    if not upload:
        raise StoreError("Flight submission has no fileUploadUrl.")
    ident = submission["id"]
    zip_path = msix.with_suffix(".zip")
    file_name = zip_msix(msix, zip_path)
    # Same order as Microsoft's walkthrough: update the data, upload, commit.
    api_json("PUT", f"{base}/{ident}", token, payload=with_package(submission, file_name), **kwargs)
    status, body = request(
        "PUT",
        upload,
        data=zip_path.read_bytes(),
        headers={"x-ms-blob-type": "BlockBlob", "Content-Type": "application/octet-stream"},
        **kwargs,
    )
    if status >= 400:
        raise StoreError(f"Azure Blob upload failed (HTTP {status}): {body.decode('utf-8', 'replace')}")
    api_json("POST", f"{base}/{ident}/commit", token, **kwargs)
    wait_for_commit(f"{base}/{ident}/status", token, sleep=sleep, **kwargs)
    return str(ident)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("msix", type=Path, help="Path to the unsigned .msix")
    args = parser.parse_args(argv)
    if not args.msix.is_file():
        print(f"error: {args.msix} is not a file", file=sys.stderr)
        return 1
    try:
        ident = submit(args.msix, env=os.environ)
    except KeyError as error:
        print(f"error: missing environment variable {error}", file=sys.stderr)
        return 1
    except StoreError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"Committed flight submission {ident}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
