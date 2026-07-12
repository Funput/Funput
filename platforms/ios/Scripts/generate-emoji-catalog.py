#!/usr/bin/env python3
"""Generate Funput's compact emoji catalog from Unicode emoji-test.txt."""

import argparse
import json
import re
from pathlib import Path

CATEGORIES = {
    "Smileys & Emotion": "smileys_people",
    "People & Body": "smileys_people",
    "Animals & Nature": "animals_nature",
    "Food & Drink": "food_drink",
    "Activities": "activities",
    "Travel & Places": "travel_places",
    "Objects": "objects",
    "Symbols": "symbols",
    "Flags": "flags",
}


def parse(source: str) -> list[dict[str, str]]:
    category = None
    items: list[dict[str, str]] = []
    seen: set[str] = set()
    for line in source.splitlines():
        if line.startswith("# group: "):
            category = CATEGORIES.get(line.removeprefix("# group: "))
            continue
        if category is None or "; fully-qualified" not in line:
            continue
        definition, comment = line.split("#", 1)
        codepoints = definition.split(";", 1)[0].split()
        if any(0x1F3FB <= int(value, 16) <= 0x1F3FF for value in codepoints):
            continue
        glyph = "".join(chr(int(value, 16)) for value in codepoints)
        match = re.match(r"\s*\S+\s+E[0-9.]+\s+(.+)", comment)
        if match is None or glyph in seen:
            continue
        seen.add(glyph)
        items.append({"glyph": glyph, "name": match.group(1), "category": category})
    return items


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--version", default="15.1")
    arguments = parser.parse_args()
    catalog = {
        "version": arguments.version,
        "emojis": parse(arguments.source.read_text(encoding="utf-8")),
    }
    arguments.output.write_text(
        json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
