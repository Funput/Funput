#!/usr/bin/env python3
"""Four-part MSIX Identity.Version from a 3-part marketing version.

Each component is a UINT16 (0–65535), and the first cannot be 0. The fourth is
reserved for the Store and must be 0 when the package is built — Partner Center
rejects anything else. So the marketing version (`1.2026.66`) *is* the package
version (`1.2026.66.0`), and every build that reaches Partner Center needs a new
marketing version. A run that failed before its commit consumed nothing.
"""

from __future__ import annotations

import argparse
import sys


class VersionError(ValueError):
    """The marketing version cannot be an MSIX Identity.Version."""


def parse_u16(part: str, label: str) -> int:
    if not part.isdigit():
        raise VersionError(f"{label} must be a number, got {part!r}")
    value = int(part, 10)
    if value > 65535:
        raise VersionError(f"{label} {value} exceeds the MSIX 16-bit maximum (65535)")
    return value


def package_version(marketing: str) -> str:
    """`1.2026.66` → `1.2026.66.0`."""
    parts = marketing.strip().split(".")
    if len(parts) != 3:
        raise VersionError(
            f"marketing version must be three numeric parts (e.g. 1.2026.66), got {marketing!r}"
        )
    major, minor, build = (parse_u16(p, name) for p, name in zip(parts, ("major", "minor", "build")))
    if major == 0:
        raise VersionError("major must not be 0 — the Store rejects a package version starting with 0")
    return f"{major}.{minor}.{build}.0"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("marketing", help="Three-part marketing version, e.g. 1.2026.66")
    args = parser.parse_args(argv)
    try:
        print(package_version(args.marketing))
    except VersionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
