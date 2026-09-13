#!/usr/bin/env python3
"""Four-part MSIX Identity.Version from a 3-part marketing version + revision.

Each component is a UINT16 (0–65535). The marketing version is what users see
(`1.2026.66`); the fourth part is `STORE_BUILD_OFFSET + github.run_number` so
re-deploying the same marketing version never collides at Partner Center.
"""

from __future__ import annotations

import argparse
import sys


class VersionError(ValueError):
    """The marketing version or revision cannot be an MSIX Identity.Version."""


def parse_u16(part: str, label: str) -> int:
    if not part.isdigit():
        raise VersionError(f"{label} must be a number, got {part!r}")
    value = int(part, 10)
    if value > 65535:
        raise VersionError(f"{label} {value} exceeds the MSIX 16-bit maximum (65535)")
    return value


def package_version(marketing: str, revision: int) -> str:
    """`1.2026.66` + `101` → `1.2026.66.101`."""
    parts = marketing.strip().split(".")
    if len(parts) != 3:
        raise VersionError(
            f"marketing version must be three numeric parts (e.g. 1.2026.66), got {marketing!r}"
        )
    major, minor, build = (parse_u16(p, name) for p, name in zip(parts, ("major", "minor", "build")))
    if revision < 0 or revision > 65535:
        raise VersionError(f"revision {revision} is outside 0..=65535")
    return f"{major}.{minor}.{build}.{revision}"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("marketing", help="Three-part marketing version, e.g. 1.2026.66")
    parser.add_argument("revision", type=int, help="Fourth part (offset + run number)")
    args = parser.parse_args(argv)
    try:
        print(package_version(args.marketing, args.revision))
    except VersionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
