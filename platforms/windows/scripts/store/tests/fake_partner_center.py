"""A stand-in for Azure AD, Partner Center and the upload blob — no network.

Shared by the submit tests. Endpoints and field names follow Microsoft's
"Manage package flight submissions" reference: `listflights` + `friendlyName`
for lookup, `flightPackages` for packages, and `/status` after `/commit`.
"""

from __future__ import annotations

import json
import sys
import tempfile
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

from submit_msix import submit  # noqa: E402

ENV = {
    "MS_STORE_TENANT_ID": "t",
    "MS_STORE_CLIENT_ID": "c",
    "MS_STORE_CLIENT_SECRET": "s",
    "MS_STORE_APP_ID": "9NAPP",
    "MS_STORE_FLIGHT_NAME": "internal",
}
BASE = "https://manage.devcenter.microsoft.com/v1.0/my/applications/9NAPP"
BLOB = "https://blob.example/upload"
MSIX_NAME = "Funput-1.2026.66.0.msix"


class Response:
    """What `urllib.request.urlopen` hands back, reduced to what the scripts read."""

    def __init__(self, status: int, payload: bytes | dict | list = b""):
        self._status = status
        self._payload = payload if isinstance(payload, bytes) else json.dumps(payload).encode()

    def getcode(self):
        return self._status

    def read(self):
        return self._payload

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False


def reply(status: int, payload: bytes | dict | list = b""):
    """An opener that answers every request the same way."""
    return lambda _req: Response(status, payload)


class FakePartnerCenter:
    """Answers the calls `submit` makes, recording them in order."""

    def __init__(self, *, flights=None, statuses=("PreProcessing",)):
        self.calls: list[tuple[str, str]] = []
        self.flights = flights if flights is not None else {"value": [{"friendlyName": "internal", "flightId": "f1"}]}
        self.pages: dict[str, dict] = {}
        self.statuses = list(statuses)
        self.submission = {"id": "sub-9", "fileUploadUrl": BLOB, "flightPackages": []}
        self.put_body: dict | None = None

    def __call__(self, req):
        method, url = req.get_method(), req.full_url
        self.calls.append((method, url))
        if "login.microsoftonline.com" in url:
            return Response(200, {"access_token": "tok"})
        if url in self.pages:
            return Response(200, self.pages[url])
        if url == f"{BASE}/listflights":
            return self.flights if isinstance(self.flights, Response) else Response(200, self.flights)
        if method == "DELETE":
            return Response(200)
        if method == "POST" and url == f"{BASE}/flights/f1/submissions":
            return Response(200, self.submission)
        if method == "PUT" and url == f"{BASE}/flights/f1/submissions/sub-9":
            self.put_body = json.loads(req.data.decode())
            return Response(200, {})
        if method == "PUT" and url == BLOB:
            return Response(201)
        if url.endswith("/sub-9/commit"):
            return Response(202, {"status": "CommitStarted"})
        if url.endswith("/sub-9/status"):
            return Response(200, self._next_status())
        raise AssertionError(f"unexpected {method} {url}")

    def _next_status(self) -> dict:
        status = self.statuses.pop(0) if len(self.statuses) > 1 else self.statuses[0]
        return status if isinstance(status, dict) else {"status": status, "statusDetails": {"errors": []}}

    def index(self, method: str, suffix: str) -> int:
        return next(i for i, (m, u) in enumerate(self.calls) if m == method and u.endswith(suffix))

    def run(self, sleeps: list | None = None) -> str:
        with tempfile.TemporaryDirectory() as tmp:
            msix = Path(tmp) / MSIX_NAME
            msix.write_bytes(b"pkg")
            sleep = sleeps.append if sleeps is not None else (lambda _s: None)
            return submit(msix, env=ENV, opener=self, sleep=sleep)
