#!/usr/bin/env python3
"""Summarize manually recorded P0 rows; never access a microphone or Android device."""
import argparse
import csv
import json
import math
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

EXPECTED = {"vi_clear": 40, "vi_names_numbers": 10, "vi_noise": 10, "en_clear": 20}
STATUSES = {"final", "start_error", "recognition_error", "no_match", "timeout", "cancelled"}
FIELDS = ("run_id device_id device_model android_api service_package service_version locale "
          "model_state airplane_mode corpus_id status ready_ms final_after_stop_ms hypothesis "
          "usable diacritic_error names_error numbers_error notes").split()


def read_csv(path, required):
    with Path(path).open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        if not reader.fieldnames or len(set(reader.fieldnames)) != len(reader.fieldnames):
            raise ValueError(f"{path}: missing or duplicate headers")
        missing = set(required) - set(reader.fieldnames)
        if missing:
            raise ValueError(f"{path}: missing columns {sorted(missing)}")
        for line, row in enumerate(reader, 2):
            if None in row or any(value is None for value in row.values()):
                raise ValueError(f"{path}:{line}: malformed CSV row")
            if any(value.strip() for value in row.values()):
                yield line, {key: value.strip() for key, value in row.items()}


def load_corpus(path):
    corpus = {}
    for line, row in read_csv(path, ["id", "group", "locale", "reference"]):
        if not row["id"] or row["id"] in corpus or not row["reference"]:
            raise ValueError(f"{path}:{line}: empty or duplicate corpus entry")
        if row["group"] not in EXPECTED:
            raise ValueError(f"{path}:{line}: unknown group")
        expected_locale = "en-US" if row["group"] == "en_clear" else "vi-VN"
        if row["locale"] != expected_locale:
            raise ValueError(f"{path}:{line}: locale differs from group")
        corpus[row["id"]] = row
    if Counter(row["group"] for row in corpus.values()) != EXPECTED:
        raise ValueError("Corpus must contain exactly 40/10/10 VI and 20 EN entries")
    return corpus


def milliseconds(value):
    if not value:
        return None
    number = float(value)
    if not math.isfinite(number) or number < 0:
        raise ValueError("timings must be finite non-negative milliseconds")
    return number


def load_measurements(path, corpus):
    rows, seen = [], set()
    for line, row in read_csv(path, FIELDS):
        try:
            for field in FIELDS[:11]:
                if not row[field]:
                    raise ValueError(f"missing {field}")
            if int(row["android_api"]) < 26:
                raise ValueError("android_api must be at least 26")
            reference = corpus[row["corpus_id"]]
            if row["locale"] != reference["locale"] or row["status"] not in STATUSES:
                raise ValueError("invalid locale or status")
            if row["model_state"] not in {"installed", "unknown", "pending", "downloadable", "unsupported"}:
                raise ValueError("invalid model_state")
            if row["airplane_mode"] not in {"yes", "no", "unknown"}:
                raise ValueError("invalid airplane_mode")
            key = (row["run_id"], row["device_id"], row["corpus_id"])
            if key in seen:
                raise ValueError("duplicate attempt; use a new run_id for retries")
            if row["status"] != "cancelled" and row["usable"] not in {"yes", "no"}:
                raise ValueError("usable must be manually marked yes/no")
            if row["status"] == "final" and not row["hypothesis"]:
                raise ValueError("empty final must be classified as no_match")
            if row["status"] != "final" and row["hypothesis"]:
                raise ValueError("only a final may supply hypothesis; never use partial")
            if not row["hypothesis"] and row["usable"] == "yes":
                raise ValueError("an empty result cannot be usable")
            for field in ["diacritic_error", "names_error", "numbers_error"]:
                if row[field] not in {"", "yes", "no", "not_applicable"}:
                    raise ValueError(f"invalid manual mark {field}")
            row["ready_ms"] = milliseconds(row["ready_ms"])
            row["final_after_stop_ms"] = milliseconds(row["final_after_stop_ms"])
            if row["status"] == "start_error" and row["ready_ms"] is not None:
                raise ValueError("start_error cannot have a ready timestamp")
            if row["status"] != "final" and row["final_after_stop_ms"] is not None:
                raise ValueError("only final may have final_after_stop_ms")
            seen.add(key)
            rows.append(row)
        except (ValueError, KeyError) as error:
            raise ValueError(f"{path}:{line}: {error}") from error
    return rows


def tokens(text):
    text = unicodedata.normalize("NFC", text).lower()
    return "".join(" " if unicodedata.category(char).startswith("P") else char
                   for char in text).split()


def distance(reference, hypothesis):
    previous = list(range(len(hypothesis) + 1))
    for i, ref in enumerate(reference, 1):
        current = [i]
        for j, hyp in enumerate(hypothesis, 1):
            current.append(min(current[-1] + 1, previous[j] + 1,
                               previous[j - 1] + (ref != hyp)))
        previous = current
    return previous[-1]


def percentile(values, fraction):
    ordered = sorted(value for value in values if value is not None)
    return ordered[math.ceil(len(ordered) * fraction) - 1] if ordered else None


def summarize(rows, corpus):
    attempts = [row for row in rows if row["status"] != "cancelled"]
    edits = words = 0
    for row in attempts:
        reference = tokens(corpus[row["corpus_id"]]["reference"])
        edits += distance(reference, tokens(row["hypothesis"]))
        words += len(reference)
    count = len(attempts)
    result = {"attempts": count, "cancelled": len(rows) - count,
              "unique_utterances": len({row["corpus_id"] for row in attempts}),
              "word_errors": edits, "reference_tokens": words,
              "wer_percent": 100 * edits / words if words else None,
              "usable_percent": 100 * sum(row["usable"] == "yes" for row in attempts) / count if count else None,
              "start_error_percent": 100 * sum(row["status"] == "start_error" for row in attempts) / count if count else None}
    for field in ["ready_ms", "final_after_stop_ms"]:
        result[field] = {"samples": sum(row[field] is not None for row in attempts),
                         "p50": percentile([row[field] for row in attempts], .5),
                         "p95": percentile([row[field] for row in attempts], .95)}
    result["manual_errors"] = {field: {"yes": sum(row[field] == "yes" for row in attempts),
                                         "checked": sum(row[field] in {"yes", "no"} for row in attempts)}
                               for field in ["diacritic_error", "names_error", "numbers_error"]}
    return result


def numerical_gate(rows, metrics):
    clear = [row for row in rows if row["status"] != "cancelled"]
    if len(clear) != 40 or any(row["airplane_mode"] == "unknown" for row in clear):
        return "PENDING"
    if any(row["airplane_mode"] != "yes" or int(row["android_api"]) < 31 for row in clear):
        return "FAIL"
    if any(row["ready_ms"] is None for row in clear if row["status"] != "start_error"):
        return "PENDING"
    if any(row["final_after_stop_ms"] is None for row in clear if row["status"] == "final"):
        return "PENDING"
    ready, final = metrics["ready_ms"]["p95"], metrics["final_after_stop_ms"]["p95"]
    if ready is None or final is None:
        return "PENDING"
    passed = metrics["wer_percent"] <= 20 and metrics["usable_percent"] >= 90 and ready <= 3000 and final <= 3000
    return "PASS" if passed else "FAIL"


def build_report(rows, corpus):
    grouped = defaultdict(list)
    metadata = {}
    for row in rows:
        key = (row["run_id"], row["device_id"])
        identity = tuple(row[field] for field in FIELDS[2:6])
        if key in metadata and metadata[key] != identity:
            raise ValueError("device/service metadata changed within one run")
        metadata[key] = identity
        grouped[key].append(row)
    reports, passing_devices = [], set()
    for (run_id, device_id), device_rows in sorted(grouped.items()):
        groups = {name: summarize([row for row in device_rows if corpus[row["corpus_id"]]["group"] == name], corpus)
                  for name in EXPECTED}
        clear = [row for row in device_rows if corpus[row["corpus_id"]]["group"] == "vi_clear"]
        gate = numerical_gate(clear, groups["vi_clear"])
        if gate == "PASS":
            passing_devices.add(device_id)
        reports.append({"run_id": run_id, "device_id": device_id, "groups": groups, "vi_clear_numerical_gate": gate})
    return {"runs": reports, "two_device_numerical_gate": "PASS" if len(passing_devices) >= 2 else "PENDING",
            "passing_device_count": len(passing_devices),
            "release_gate": "NOT_EVALUATED: manually verify offline/lifecycle/race evidence; numerical scores are insufficient"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, default=Path(__file__).with_name("corpus.csv"))
    parser.add_argument("--measurements", type=Path, required=True)
    parser.add_argument("--require-two-device-gate", action="store_true")
    args = parser.parse_args()
    try:
        corpus = load_corpus(args.corpus)
        report = build_report(load_measurements(args.measurements, corpus), corpus)
    except (ValueError, OSError) as error:
        parser.error(str(error))
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 1 if args.require_two_device_gate and report["two_device_numerical_gate"] != "PASS" else 0


if __name__ == "__main__":
    raise SystemExit(main())
