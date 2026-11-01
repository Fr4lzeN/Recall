import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import analyze_search_diagnostics as analyzer


def vector(values):
    finite = all(value is not None for value in values)
    return {
        "dimensions": len(values), "values": values,
        "nonFiniteCount": sum(value is None for value in values),
        "norm": analyzer.math.hypot(*values) if finite else None,
        "min": min((value for value in values if value is not None), default=None),
        "max": max((value for value in values if value is not None), default=None),
        "sha256Float32LittleEndian": "example-hash",
    }


def candidate(media_id, score, values=None, decision="displayed"):
    return {
        "mediaId": media_id, "rank": media_id, "score": score, "distance": 1 - score,
        "disposition": decision, "uri": "content://media/" + str(media_id),
        "displayName": "кот_" + str(media_id) + ".jpg",
        "vector": vector(values or [1, 0]), "exactCosine": score, "scoreDelta": 0,
    }


def trace(query="кот", timestamp=1):
    return {
        "schemaVersion": 1, "traceId": "trace-" + str(timestamp),
        "timestampMillis": timestamp, "durationMillis": 12, "query": query,
        "queryCharacterCount": len(query), "queryCaptureTruncated": False,
        "status": "success", "error": None,
        "model": {"name": "MobileCLIP", "id": "model-a", "dimensions": 2},
        "index": {"type": "SegmentedVectorIndex", "sizeAtStart": 3, "sizeAtEnd": 3,
                  "indexedMediaCount": 3, "totalMediaCount": 3},
        "minSimilarity": 0.2, "queryVector": vector([1, 0]),
        "text": {"normalizedText": query.lower(), "tokenIds": [49406, 1, 49407, 0],
                 "tokenPieces": ["<SOT>", "кот</w>", "<EOT>", "<PAD>"],
                 "originalTokenCount": 1, "truncated": False},
        "roundCount": 1,
        "rounds": [{"requested": 3, "returned": 3, "durationMillis": 1,
                    "candidateIds": [1, 2, 3], "candidateScores": [1, 0.8, 0.1], "captureTruncated": False}],
        "uniqueCandidateCount": 3, "candidateLimit": 200, "capturedCandidateCount": 3,
        "candidateCaptureTruncated": False,
        "candidates": [candidate(1, 1), candidate(2, 0.8, [0.8, 0.6]),
                       candidate(3, 0.1, [0, 1], "below_threshold")],
        "displayedIds": [2, 1],
        "exactReference": {"available": True, "reason": None, "snapshotCount": 3,
                           "snapshotRevision": 1, "isStable": True, "searchMethod": "exact",
                           "comparedTopK": 3, "recallAtK": 1.0, "top": []},
    }


class SearchDiagnosticAnalyzerTest(unittest.TestCase):
    def test_directory_unicode_traces_are_sorted_by_timestamp_and_cli_shows_last(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "z.json").write_text(json.dumps(trace(timestamp=1)), encoding="utf-8")
            (root / "a.json").write_text(json.dumps(trace("семья на пляже", 2)), encoding="utf-8")
            records = analyzer.load_traces(root)
            self.assertEqual([1, 2], [record["timestampMillis"] for record in records])
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                self.assertEqual(0, analyzer.main([directory, "--last", "1", "--limit", "1"]))
            self.assertIn("семья на пляже", output.getvalue())
            self.assertNotIn('Query: "кот"', output.getvalue())

    def test_zip_is_read_without_extracting_member_names(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "export.zip"
            with zipfile.ZipFile(source, "w") as archive:
                archive.writestr("../../must-not-be-extracted.json", json.dumps(trace()))
                archive.writestr("manifest.json", "{}")
            records = analyzer.load_traces(source)
            self.assertEqual("кот", records[0]["query"])
            self.assertEqual([source], list(Path(directory).iterdir()))

    def test_displayed_order_and_recomputed_cosine_not_raw_candidate_order(self):
        record = trace()
        record["candidates"][1]["score"] = 0.9
        rendered = analyzer.format_trace(record)
        self.assertLess(rendered.index("1. id=2"), rendered.index("2. id=1"))
        self.assertIn('"кот_2.jpg"', rendered)
        self.assertIn("score=0.900000 cosine=0.800000 delta=0.100000", rendered)
        self.assertIn("below_threshold=1", rendered)
        self.assertNotIn('"values":', rendered)

    def test_math_handles_orthogonal_opposite_zero_and_dimension_mismatch(self):
        self.assertEqual(1, analyzer.cosine_similarity([3, 4], [6, 8]))
        self.assertEqual(0, analyzer.cosine_similarity([1, 0], [0, 1]))
        self.assertEqual(-1, analyzer.cosine_similarity([1, 0], [-1, 0]))
        for left, right in [([0, 0], [1, 0]), ([1], [1, 0]), ([float("nan")], [1]), ([float("inf")], [1])]:
            with self.assertRaises(analyzer.DiagnosticError):
                analyzer.cosine_similarity(left, right)

    def test_nonfinite_vector_components_are_reported_and_not_used_for_cosine(self):
        record = trace()
        record["queryVector"] = vector([None, 1])
        analyzer.validate_trace(record)
        rendered = analyzer.format_trace(record)
        self.assertIn("nonFiniteCount=1", rendered)
        self.assertIn("cosine=n/a (vector contains nonfinite components)", rendered)

    def test_unstable_reference_never_displays_recall_as_valid(self):
        record = trace()
        record["exactReference"]["isStable"] = False
        record["exactReference"]["reason"] = "index_changed_during_search"
        output = analyzer.format_trace(record)
        self.assertIn("ANN recall: not comparable (index_changed_during_search)", output)
        self.assertNotIn("ANN recall@3=1.000000", output)
        record["exactReference"]["isStable"] = True
        self.assertIn("ANN recall@3=1.000000", analyzer.format_trace(record))

    def test_capture_limits_make_partial_decision_counts_explicit(self):
        record = trace()
        record["uniqueCandidateCount"] = 600
        record["candidateCaptureTruncated"] = True
        output = analyzer.format_trace(record, limit=1, media_id=3)
        self.assertIn("Capture: 3/600", output)
        self.assertIn("decision counts cover captured candidates only", output)
        self.assertIn("1 more displayed images", output)
        self.assertIn("Requested media ID 3", output)
        self.assertIn("decision=below_threshold", output)

    def test_latest_exact_query_comparison_math_and_overlap(self):
        old = trace("кот", 1)
        old["queryVector"] = vector([0, 1])
        left = trace("кот", 2)
        right = trace("cat", 3)
        right["queryVector"] = vector([0.8, 0.6])
        right["displayedIds"] = [1, 3]
        output = analyzer.format_comparison([old, left, right], ["кот", "cat"], limit=2)
        self.assertIn("Text-vector cosine: 0.800000", output)
        self.assertIn("common=1 left=2 right=2 jaccard=0.333333", output)
        self.assertIn("do not establish semantic or language accuracy", output)

    def test_different_models_and_truncated_queries_are_not_silently_compared(self):
        left, right = trace("кот", 1), trace("cat", 2)
        right["model"]["id"] = "model-b"
        self.assertIn("model IDs differ", analyzer.format_comparison([left, right], ["кот", "cat"]))
        left["queryCaptureTruncated"] = True
        with self.assertRaises(analyzer.DiagnosticError):
            analyzer.format_comparison([left, right], ["кот", "cat"])

    def test_malformed_schema_and_nonstandard_nan_are_rejected(self):
        for mutation in [lambda r: r.update(schemaVersion=2),
                         lambda r: r["queryVector"].update(dimensions=3),
                         lambda r: r["text"].update(tokenPieces=["one"]),
                         lambda r: r["candidates"][0].update(score=float("nan"))]:
            record = trace()
            mutation(record)
            with self.assertRaises(analyzer.DiagnosticError):
                analyzer.validate_trace(record)
        with self.assertRaises(analyzer.DiagnosticError):
            analyzer._decode_trace(b'{"schemaVersion":1,"value":NaN}', "test.json")

    def test_per_trace_and_total_limits_apply_to_directory_and_zip(self):
        payload = json.dumps(trace()).encode()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "one.json").write_bytes(payload)
            with patch.object(analyzer, "MAX_TRACE_BYTES", len(payload) - 1):
                with self.assertRaises(analyzer.DiagnosticError):
                    analyzer.load_traces(root)
            source = root / "export.zip"
            with zipfile.ZipFile(source, "w") as archive:
                archive.writestr("one.json", payload)
                archive.writestr("two.json", payload)
            with patch.object(analyzer, "MAX_TOTAL_BYTES", len(payload) + 1):
                with self.assertRaises(analyzer.DiagnosticError):
                    analyzer.load_traces(source)

    def test_missing_requested_media_is_not_reported_as_proven_irrelevance(self):
        output = analyzer.format_trace(trace(), media_id=999)
        self.assertIn("absence does not prove irrelevance", output)

    def test_failed_final_size_read_is_unknown_not_empty_or_invalid_trace(self):
        record = trace()
        record["index"]["sizeAtEnd"] = -1
        analyzer.validate_trace(record)
        self.assertIn("size=3->unknown", analyzer.format_trace(record))
        record["index"]["sizeAtEnd"] = -2
        with self.assertRaises(analyzer.DiagnosticError):
            analyzer.validate_trace(record)


if __name__ == "__main__":
    unittest.main()
