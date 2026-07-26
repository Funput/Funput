#!/usr/bin/env python3
"""Generate Funput's emoji catalog from Unicode Emoji and CLDR annotations."""

import argparse
import json
import re
import xml.etree.ElementTree as ET
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


def parse_emoji(source: str) -> list[dict[str, object]]:
    category = None
    items: list[dict[str, object]] = []
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


def parse_annotations(paths: list[Path]) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    for path in paths:
        if not path.exists():
            continue
        for node in ET.parse(path).iterfind(".//annotation"):
            glyph = node.attrib.get("cp")
            value = (node.text or "").strip()
            if not glyph or not value:
                continue
            annotation = result.setdefault(glyph, {"keywords": []})
            if node.attrib.get("type") == "tts":
                annotation["name"] = value
            else:
                annotation["keywords"] = [
                    term.strip() for term in value.split("|") if term.strip()
                ]
    return result


def localized_catalog(
    items: list[dict[str, object]],
    english: dict[str, dict[str, object]],
    vietnamese: dict[str, dict[str, object]],
) -> list[dict[str, object]]:
    for item in items:
        glyph = str(item["glyph"])
        english_values = english.get(glyph, {})
        vietnamese_values = vietnamese.get(glyph, {})
        localized_name = vietnamese_values.get("name")
        if localized_name:
            item["localizedName"] = localized_name
        terms = [
            english_values.get("name"),
            vietnamese_values.get("name"),
            *english_values.get("keywords", []),
            *vietnamese_values.get("keywords", []),
        ]
        item["searchTerms"] = list(
            dict.fromkeys(str(term) for term in terms if term)
        )
    return items


def annotation_paths(root: Path, locale: str) -> list[Path]:
    return [
        root / "common" / "annotations" / f"{locale}.xml",
        root / "common" / "annotationsDerived" / f"{locale}.xml",
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--version", default="15.1")
    parser.add_argument("--cldr-root", type=Path)
    parser.add_argument("--cldr-version", default="")
    arguments = parser.parse_args()
    items = parse_emoji(arguments.source.read_text(encoding="utf-8"))
    if arguments.cldr_root:
        paths = annotation_paths(arguments.cldr_root, "en") + annotation_paths(
            arguments.cldr_root, "vi"
        )
        missing = [str(path) for path in paths if not path.exists()]
        if missing:
            parser.error("missing CLDR annotation files: " + ", ".join(missing))
        if not arguments.cldr_version:
            parser.error("--cldr-version is required with --cldr-root")
        english = parse_annotations(paths[:2])
        vietnamese = parse_annotations(paths[2:])
        items = localized_catalog(items, english, vietnamese)
    catalog = {
        "version": arguments.version,
        "annotationVersion": arguments.cldr_version,
        "emojis": items,
    }
    arguments.output.write_text(
        json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
