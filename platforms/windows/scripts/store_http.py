"""HTTP helpers for Partner Center and Azure AD. Stdlib only."""

from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

from store_flight import BOOTSTRAP_HINT, bootstrap_required

TOKEN_SCOPE = "https://manage.devcenter.microsoft.com/.default"
API_ROOT = "https://manage.devcenter.microsoft.com/v1.0/my"


class StoreError(RuntimeError):
    def __init__(self, message: str, *, bootstrap: bool = False) -> None:
        super().__init__(message)
        self.bootstrap = bootstrap


def request(
    method: str,
    url: str,
    *,
    token: str | None = None,
    data: bytes | None = None,
    headers: dict[str, str] | None = None,
    opener=urllib.request.urlopen,
) -> tuple[int, bytes]:
    req_headers = dict(headers or {})
    if token:
        req_headers["Authorization"] = f"Bearer {token}"
    http_request = urllib.request.Request(url, data=data, method=method, headers=req_headers)
    try:
        with opener(http_request) as response:
            return response.getcode(), response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()


def access_token(tenant: str, client_id: str, client_secret: str, opener=urllib.request.urlopen) -> str:
    url = f"https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token"
    payload = urllib.parse.urlencode(
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            "scope": TOKEN_SCOPE,
        }
    ).encode()
    status, body = request(
        "POST",
        url,
        data=payload,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        opener=opener,
    )
    if status != 200:
        raise StoreError(f"Azure AD refused a token (HTTP {status}): {body.decode('utf-8', 'replace')}")
    token = json.loads(body).get("access_token")
    if not token:
        raise StoreError("Azure AD returned no access_token.")
    return token


def api_json(
    method: str,
    path: str,
    token: str,
    *,
    payload: Any | None = None,
    opener=urllib.request.urlopen,
) -> Any:
    data = None if payload is None else json.dumps(payload).encode()
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    status, body = request(
        method, f"{API_ROOT}{path}", token=token, data=data, headers=headers, opener=opener
    )
    text = body.decode("utf-8", "replace")
    if bootstrap_required(status, text):
        raise StoreError(f"{BOOTSTRAP_HINT}\nAPI HTTP {status}: {text}", bootstrap=True)
    if status >= 400:
        raise StoreError(f"Partner Center HTTP {status} on {method} {path}: {text}")
    return json.loads(text) if text else {}
