#!/usr/bin/env python3
"""Upload an .msix to a Partner Center package flight and commit it.

Uploading is not publishing to production. The flight's testers see the build;
promoting it still happens in Partner Center.

Required environment: MS_STORE_TENANT_ID, MS_STORE_CLIENT_ID,
MS_STORE_CLIENT_SECRET, MS_STORE_APP_ID, MS_STORE_FLIGHT_NAME.
"""

from __future__ import annotations

import argparse
import os
import sys
import zipfile
from pathlib import Path

from store_flight import as_flight_list, find_flight, new_flight_body, pending_submission_id, with_package
from store_http import StoreError, access_token, api_json, request


def zip_msix(msix: Path, dest: Path) -> str:
    name = msix.name
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(msix, arcname=name)
    return name


def submit(msix: Path, *, env: dict[str, str], opener=None) -> str:
    kwargs = {} if opener is None else {"opener": opener}
    token = access_token(env["MS_STORE_TENANT_ID"], env["MS_STORE_CLIENT_ID"], env["MS_STORE_CLIENT_SECRET"], **kwargs)
    app_id, flight_name = env["MS_STORE_APP_ID"], env["MS_STORE_FLIGHT_NAME"]
    listed = api_json("GET", f"/applications/{app_id}/flights", token, **kwargs)
    flight = find_flight(as_flight_list(listed), flight_name)
    if flight is None:
        flight = api_json(
            "POST",
            f"/applications/{app_id}/flights",
            token,
            payload=new_flight_body(flight_name),
            **kwargs,
        )
    flight_id = flight["flightId"]
    pending = pending_submission_id(flight)
    if pending:
        api_json("DELETE", f"/applications/{app_id}/flights/{flight_id}/submissions/{pending}", token, **kwargs)
    submission = api_json("POST", f"/applications/{app_id}/flights/{flight_id}/submissions", token, **kwargs)
    upload = submission.get("fileUploadUrl")
    if not upload:
        raise StoreError("Flight submission has no fileUploadUrl.")
    zip_path = msix.with_suffix(".zip")
    file_name = zip_msix(msix, zip_path)
    status, body = request(
        "PUT",
        upload,
        data=zip_path.read_bytes(),
        headers={"x-ms-blob-type": "BlockBlob", "Content-Type": "application/octet-stream"},
        **kwargs,
    )
    if status >= 400:
        raise StoreError(f"Azure Blob upload failed (HTTP {status}): {body.decode('utf-8', 'replace')}")
    updated = with_package(submission, file_name)
    ident = submission["id"]
    api_json("PUT", f"/applications/{app_id}/flights/{flight_id}/submissions/{ident}", token, payload=updated, **kwargs)
    api_json("POST", f"/applications/{app_id}/flights/{flight_id}/submissions/{ident}/commit", token, **kwargs)
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
