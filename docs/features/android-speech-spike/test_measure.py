"""Gate regression tests with synthetic in-memory rows, never device measurements."""
import copy
import csv
import tempfile
import unittest
from pathlib import Path

import measure


class SpeechMeasurementTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.corpus = measure.load_corpus(Path(__file__).with_name("corpus.csv"))

    def clear_rows(self, device="synthetic-a", run="fixture"):
        rows = []
        for item in self.corpus.values():
            if item["group"] != "vi_clear":
                continue
            row = dict.fromkeys(measure.FIELDS, "")
            row.update(run_id=run, device_id=device, device_model="test-only",
                       android_api="35", service_package="synthetic.provider",
                       service_version="fixture", locale="vi-VN", model_state="installed",
                       airplane_mode="yes", corpus_id=item["id"], status="final",
                       ready_ms=100, final_after_stop_ms=200,
                       hypothesis=item["reference"], usable="yes")
            rows.append(row)
        return rows

    def report(self, rows):
        return measure.build_report(rows, self.corpus)

    def test_empty_measurements_do_not_pass(self):
        report = self.report([])
        self.assertEqual("PENDING", report["two_device_numerical_gate"])
        self.assertTrue(report["release_gate"].startswith("NOT_EVALUATED"))

    def test_two_runs_on_one_device_do_not_replace_two_devices(self):
        rows = self.clear_rows(run="first") + self.clear_rows(run="retry")
        self.assertEqual(1, self.report(rows)["passing_device_count"])
        rows += self.clear_rows(device="synthetic-b")
        report = self.report(rows)
        self.assertEqual("PASS", report["two_device_numerical_gate"])
        self.assertTrue(report["release_gate"].startswith("NOT_EVALUATED"))

    def test_missing_timing_or_corpus_remains_pending(self):
        rows = self.clear_rows()
        rows[0]["ready_ms"] = None
        self.assertEqual("PENDING", self.report(rows)["runs"][0]["vi_clear_numerical_gate"])
        self.assertEqual("PENDING", self.report(rows[1:])["runs"][0]["vi_clear_numerical_gate"])

    def test_quality_latency_and_offline_thresholds_are_enforced(self):
        base = self.clear_rows()
        mutations = [dict(airplane_mode="no"), dict(android_api="30"),
                     dict(hypothesis="sai", usable="no"), dict(ready_ms=3001),
                     dict(final_after_stop_ms=3001)]
        for mutation in mutations:
            with self.subTest(mutation=mutation):
                rows = copy.deepcopy(base)
                for row in rows:
                    row.update(mutation)
                self.assertEqual("FAIL", self.report(rows)["runs"][0]["vi_clear_numerical_gate"])

    def test_normalization_keeps_diacritics_and_number_representation(self):
        self.assertEqual(measure.tokens("HÔM NAY!"), measure.tokens("hôm nay."))
        self.assertEqual(measure.tokens("e\u0301"), measure.tokens("é"))
        self.assertNotEqual(measure.tokens("má"), measure.tokens("ma"))
        self.assertNotEqual(measure.tokens("12"), measure.tokens("mười hai"))
        self.assertEqual(38, measure.percentile(list(range(1, 41)), .95))

    def test_duplicate_partial_and_invalid_timing_rows_are_rejected(self):
        base = self.clear_rows()[0]
        invalid_rows = [
            [base, base],
            [dict(base, status="recognition_error")],
            [dict(base, ready_ms="nan")],
            [dict(base, final_after_stop_ms="-1")],
            [dict(base, hypothesis="")],
        ]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "synthetic.csv"
            for rows in invalid_rows:
                with self.subTest(rows=rows):
                    with path.open("w", encoding="utf-8", newline="") as stream:
                        writer = csv.DictWriter(stream, fieldnames=measure.FIELDS)
                        writer.writeheader()
                        writer.writerows(rows)
                    with self.assertRaises(ValueError):
                        measure.load_measurements(path, self.corpus)


if __name__ == "__main__":
    unittest.main()
