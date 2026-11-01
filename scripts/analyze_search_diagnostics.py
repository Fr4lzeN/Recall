#!/usr/bin/env python3
"""Inspect Recall search-diagnostic exports without third-party dependencies."""

import argparse
from collections import Counter
from datetime import datetime, timezone
import json
import math
from pathlib import Path, PurePosixPath
import sys
import zipfile


MAX_TRACE_BYTES = 8 * 1024 * 1024
MAX_TOTAL_BYTES = 64 * 1024 * 1024
MAX_TRACE_FILES = 4096
MAX_VECTOR_DIMENSIONS = 16384


class DiagnosticError(ValueError):
    pass


def _integer(value, name, minimum=0):
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        raise DiagnosticError(name + " must be an integer >= " + str(minimum))
    return value


def _number(value, name, nullable=False):
    if value is None and nullable:
        return None
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        raise DiagnosticError(name + " must be a finite number")
    return value


def _text(value, name, nullable=False):
    if value is None and nullable:
        return None
    if not isinstance(value, str):
        raise DiagnosticError(name + " must be a string")
    return value


def _object(value, name):
    if not isinstance(value, dict):
        raise DiagnosticError(name + " must be an object")
    return value


def _array(value, name):
    if not isinstance(value, list):
        raise DiagnosticError(name + " must be an array")
    return value


def _boolean(value, name, nullable=False):
    if value is None and nullable:
        return None
    if not isinstance(value, bool):
        raise DiagnosticError(name + " must be a boolean")
    return value


def validate_vector(value, name):
    if value is None:
        return
    vector = _object(value, name)
    count = _integer(vector.get("dimensions"), name + ".dimensions")
    if count > MAX_VECTOR_DIMENSIONS:
        raise DiagnosticError(name + " exceeds the vector dimension limit")
    values = _array(vector.get("values"), name + ".values")
    if len(values) != count:
        raise DiagnosticError(name + ".values length differs from dimensions")
    for component in values:
        _number(component, name + " component", nullable=True)
    nonfinite = _integer(vector.get("nonFiniteCount", 0), name + ".nonFiniteCount")
    if nonfinite != sum(component is None for component in values):
        raise DiagnosticError(name + ".nonFiniteCount differs from captured null components")
    _number(vector.get("norm"), name + ".norm", nullable=True)
    for key in ("min", "max"):
        _number(vector.get(key), name + "." + key, nullable=True)
    _text(vector.get("sha256Float32LittleEndian"), name + ".sha256Float32LittleEndian", nullable=True)


def _validate_candidate(candidate, name):
    candidate = _object(candidate, name)
    _integer(candidate.get("mediaId"), name + ".mediaId")
    if candidate.get("rank") is not None:
        _integer(candidate["rank"], name + ".rank", 1)
    _text(candidate.get("disposition"), name + ".disposition")
    for key in ("uri", "displayName"):
        _text(candidate.get(key), name + "." + key, nullable=True)
    for key in ("score", "distance", "exactCosine", "scoreDelta"):
        _number(candidate.get(key), name + "." + key, nullable=True)
    validate_vector(candidate.get("vector"), name + ".vector")


def validate_trace(trace):
    trace = _object(trace, "trace")
    if isinstance(trace.get("schemaVersion"), bool) or trace.get("schemaVersion") != 1:
        raise DiagnosticError("unsupported diagnostic schemaVersion")
    for key in ("traceId", "query", "status"):
        _text(trace.get(key), key)
    _integer(trace.get("timestampMillis"), "timestampMillis")
    _number(trace.get("durationMillis"), "durationMillis")
    _text(trace.get("error"), "error", nullable=True)
    model = _object(trace.get("model"), "model")
    _text(model.get("name"), "model.name")
    _text(model.get("id"), "model.id")
    _integer(model.get("dimensions"), "model.dimensions", 1)
    index = _object(trace.get("index"), "index")
    _text(index.get("type"), "index.type")
    for key in ("sizeAtStart", "indexedMediaCount", "totalMediaCount"):
        _integer(index.get(key), "index." + key)
    # A failed size read during finalization is captured as -1, not an empty index.
    _integer(index.get("sizeAtEnd"), "index.sizeAtEnd", -1)
    _number(trace.get("minSimilarity"), "minSimilarity", nullable=True)
    validate_vector(trace.get("queryVector"), "queryVector")
    if trace.get("text") is not None:
        text = _object(trace["text"], "text")
        _text(text.get("normalizedText"), "text.normalizedText", nullable=True)
        ids = text.get("tokenIds")
        pieces = text.get("tokenPieces")
        if ids is not None:
            for token in _array(ids, "text.tokenIds"):
                _integer(token, "token ID")
        if pieces is not None:
            for piece in _array(pieces, "text.tokenPieces"):
                _text(piece, "token piece")
        if ids is not None and pieces is not None and len(ids) != len(pieces):
            raise DiagnosticError("token IDs and pieces have different lengths")
        if text.get("originalTokenCount") is not None:
            _integer(text["originalTokenCount"], "text.originalTokenCount")
        _boolean(text.get("truncated"), "text.truncated", nullable=True)
    for i, candidate in enumerate(_array(trace.get("candidates"), "candidates")):
        _validate_candidate(candidate, "candidates[" + str(i) + "]")
    for media_id in _array(trace.get("displayedIds"), "displayedIds"):
        _integer(media_id, "displayed media ID")
    for key in ("uniqueCandidateCount", "capturedCandidateCount", "candidateLimit", "roundCount"):
        _integer(trace.get(key), key)
    for key in ("candidateCaptureTruncated", "queryCaptureTruncated"):
        _boolean(trace.get(key, False), key)
    rounds = _array(trace.get("rounds"), "rounds")
    for round_info in rounds:
        round_info = _object(round_info, "round")
        for key in ("requested", "returned", "durationMillis"):
            _integer(round_info.get(key), "round." + key)
        for media_id in _array(round_info.get("candidateIds"), "round.candidateIds"):
            _integer(media_id, "round candidate ID")
        for score in _array(round_info.get("candidateScores"), "round.candidateScores"):
            _number(score, "round score", nullable=True)
    if trace.get("exactReference") is not None:
        reference = _object(trace["exactReference"], "exactReference")
        _boolean(reference.get("available"), "exactReference.available")
        _boolean(reference.get("isStable"), "exactReference.isStable")
        _integer(reference.get("snapshotCount"), "exactReference.snapshotCount")
        _integer(reference.get("comparedTopK"), "exactReference.comparedTopK")
        _text(reference.get("searchMethod"), "exactReference.searchMethod", nullable=True)
        _text(reference.get("reason"), "exactReference.reason", nullable=True)
        _number(reference.get("recallAtK"), "exactReference.recallAtK", nullable=True)
        for candidate in _array(reference.get("top"), "exactReference.top"):
            _validate_candidate(candidate, "exact reference candidate")
    return trace


def _reject_constant(value):
    raise DiagnosticError("JSON contains forbidden nonfinite literal " + value)


def _decode_trace(data, name):
    try:
        trace = json.loads(data.decode("utf-8"), parse_constant=_reject_constant)
        return validate_trace(trace)
    except (UnicodeError, json.JSONDecodeError, DiagnosticError) as error:
        raise DiagnosticError(str(name) + ": " + str(error)) from error


def load_traces(source):
    """Read bounded JSON files; ZIP names are never extracted to the filesystem."""
    source = Path(source)
    records = []
    total = 0

    def accept(data, name):
        nonlocal total
        if len(data) > MAX_TRACE_BYTES:
            raise DiagnosticError(str(name) + " exceeds the 8 MiB trace limit")
        total += len(data)
        if total > MAX_TOTAL_BYTES:
            raise DiagnosticError("diagnostic JSON exceeds the 64 MiB total limit")
        records.append(_decode_trace(data, name))

    if source.is_dir():
        paths = sorted(p for p in source.rglob("*.json") if p.name != "manifest.json")
        if len(paths) > MAX_TRACE_FILES:
            raise DiagnosticError("too many diagnostic JSON files")
        for path in paths:
            if path.is_symlink():
                raise DiagnosticError("symbolic-link trace files are not supported: " + str(path))
            if path.stat().st_size > MAX_TRACE_BYTES:
                raise DiagnosticError(str(path) + " exceeds the 8 MiB trace limit")
            with path.open("rb") as stream:
                accept(stream.read(MAX_TRACE_BYTES + 1), path)
    else:
        with zipfile.ZipFile(source) as archive:
            members = [info for info in archive.infolist()
                       if not info.is_dir() and info.filename.endswith(".json")
                       and PurePosixPath(info.filename).name != "manifest.json"]
            if len(members) > MAX_TRACE_FILES:
                raise DiagnosticError("too many diagnostic JSON files")
            if any(info.file_size > MAX_TRACE_BYTES for info in members):
                raise DiagnosticError("ZIP contains a trace exceeding the 8 MiB limit")
            if sum(info.file_size for info in members) > MAX_TOTAL_BYTES:
                raise DiagnosticError("ZIP diagnostic JSON exceeds the 64 MiB total limit")
            for info in members:
                with archive.open(info) as stream:
                    accept(stream.read(MAX_TRACE_BYTES + 1), info.filename)
    if not records:
        raise DiagnosticError("no schema v1 search traces found")
    return sorted(records, key=lambda record: (record["timestampMillis"], record["traceId"]))


def vector_values(vector):
    if vector is None:
        raise DiagnosticError("vector was not captured")
    values = vector["values"]
    if not values:
        raise DiagnosticError("empty vector")
    if any(value is None or isinstance(value, bool) or not isinstance(value, (int, float))
           or not math.isfinite(value) for value in values):
        raise DiagnosticError("vector contains nonfinite components")
    return values


def cosine_similarity(left, right):
    if len(left) != len(right) or not left:
        raise DiagnosticError("vector dimensions differ or are empty")
    for value in (*left, *right):
        _number(value, "cosine component")
    left_norm = math.hypot(*left)
    right_norm = math.hypot(*right)
    if not math.isfinite(left_norm) or not math.isfinite(right_norm):
        raise DiagnosticError("vector norm is not finite")
    if left_norm == 0 or right_norm == 0:
        raise DiagnosticError("zero vector norm")
    result = math.fsum((a / left_norm) * (b / right_norm) for a, b in zip(left, right))
    return max(-1.0, min(1.0, result))


def _format_number(value):
    return "n/a" if value is None else format(value, ".6f")


def _quoted(value):
    return json.dumps(value, ensure_ascii=False)


def _candidate_map(trace):
    candidates = {}
    reference = trace.get("exactReference") or {}
    for candidate in reference.get("top", []):
        candidates.setdefault(candidate["mediaId"], candidate)
    for candidate in trace["candidates"]:
        media_id = candidate["mediaId"]
        if media_id not in candidates or candidate["disposition"] == "displayed":
            candidates[media_id] = candidate
        elif candidates[media_id]["disposition"] == "exact_reference":
            candidates[media_id] = candidate
    return candidates


def _candidate_line(candidate, query_vector, prefix):
    try:
        cosine = cosine_similarity(vector_values(query_vector), vector_values(candidate.get("vector")))
        delta = None if candidate.get("score") is None else candidate["score"] - cosine
        computed = "cosine=" + _format_number(cosine) + " delta=" + _format_number(delta)
    except DiagnosticError as error:
        computed = "cosine=n/a (" + str(error) + ")"
    return (prefix + "id=" + str(candidate["mediaId"]) + " "
            + _quoted(candidate.get("displayName")) + " score=" + _format_number(candidate.get("score"))
            + " " + computed + " decision=" + candidate["disposition"] + "\n"
            + "    uri=" + _quoted(candidate.get("uri")))


def format_trace(trace, limit=10, media_id=None):
    timestamp = datetime.fromtimestamp(trace["timestampMillis"] / 1000, timezone.utc).isoformat()
    model = trace["model"]
    index = trace["index"]
    lines = [
        timestamp + " trace=" + trace["traceId"],
        "Query: " + _quoted(trace["query"]),
        "Model: " + model["name"] + " id=" + model["id"] + " dimensions=" + str(model["dimensions"]),
        "Status: " + trace["status"] + " durationMs=" + str(trace["durationMillis"])
        + " minSimilarity=" + _format_number(trace.get("minSimilarity")),
        "Index: " + index["type"] + " size=" + str(index["sizeAtStart"]) + "->"
        + ("unknown" if index["sizeAtEnd"] == -1 else str(index["sizeAtEnd"]))
        + " indexedMedia=" + str(index["indexedMediaCount"])
        + "/" + str(index["totalMediaCount"]),
    ]
    if trace.get("error"):
        lines.append("Error: " + _quoted(trace["error"]))
    text = trace.get("text")
    if text:
        pieces = text.get("tokenPieces")
        lines.append("Text: normalized=" + _quoted(text.get("normalizedText"))
                     + " originalContentTokens=" + str(text.get("originalTokenCount"))
                     + " encodedPositions=" + str(len(text.get("tokenIds") or []))
                     + " truncated=" + str(text.get("truncated")))
        if pieces:
            content = [piece for piece in pieces if piece not in ("<PAD>",)]
            lines.append("Token pieces: " + _quoted(content))
    vector = trace.get("queryVector")
    if vector:
        try:
            norm = math.hypot(*vector_values(vector))
            norm_text = _format_number(norm) if math.isfinite(norm) else "not finite"
        except DiagnosticError as error:
            norm_text = "n/a (" + str(error) + ")"
        lines.append("Query vector: norm=" + norm_text + " nonFiniteCount="
                     + str(vector.get("nonFiniteCount", 0)) + " sha256="
                     + str(vector.get("sha256Float32LittleEndian")))
    else:
        lines.append("Query vector: not captured")
    reference = trace.get("exactReference")
    if reference and reference["available"]:
        lines.append("Exact reference: method=" + str(reference.get("searchMethod"))
                     + " stable=" + str(reference["isStable"])
                     + " snapshotVectors=" + str(reference["snapshotCount"]))
        if reference["isStable"] and reference.get("recallAtK") is not None:
            lines.append("ANN recall@" + str(reference["comparedTopK"]) + "="
                         + _format_number(reference["recallAtK"]) + " (unfiltered vector ranking)")
        else:
            lines.append("ANN recall: not comparable (" + str(reference.get("reason") or "no valid comparison") + ")")
    else:
        lines.append("Exact reference: unavailable (" + str((reference or {}).get("reason") or "not captured") + ")")
    counts = Counter(candidate["disposition"] for candidate in trace["candidates"])
    lines.append("Captured decisions: " + ", ".join(name + "=" + str(counts[name]) for name in sorted(counts)))
    lines.append("Capture: " + str(trace["capturedCandidateCount"]) + "/" + str(trace["uniqueCandidateCount"])
                 + " candidates, limit=" + str(trace["candidateLimit"])
                 + ", truncated=" + str(trace["candidateCaptureTruncated"])
                 + (" (decision counts cover captured candidates only)" if trace["candidateCaptureTruncated"] else ""))
    if trace.get("queryCaptureTruncated"):
        lines.append("Query capture is truncated; the stored query is only a prefix.")
    lookup = _candidate_map(trace)
    lines.append("Displayed images: " + str(len(trace["displayedIds"])))
    for position, current_id in enumerate(trace["displayedIds"][:limit], 1):
        candidate = lookup.get(current_id)
        if candidate is None:
            lines.append("  " + str(position) + ". id=" + str(current_id) + " metadata/vector not captured")
        else:
            lines.append(_candidate_line(candidate, vector, "  " + str(position) + ". "))
    if len(trace["displayedIds"]) > limit:
        lines.append("  ... " + str(len(trace["displayedIds"]) - limit) + " more displayed images; raise --limit to inspect")
    if media_id is not None:
        candidate = lookup.get(media_id)
        lines.append("Requested media ID " + str(media_id) + ":")
        if candidate:
            lines.append(_candidate_line(candidate, vector, "  "))
            image_vector = candidate.get("vector")
            if image_vector:
                lines.append("    vector norm=" + _format_number(image_vector.get("norm"))
                             + " nonFiniteCount=" + str(image_vector.get("nonFiniteCount", 0))
                             + " sha256=" + str(image_vector.get("sha256Float32LittleEndian")))
        else:
            lines.append("  Not captured in candidates or the exact reference; absence does not prove irrelevance.")
    return "\n".join(lines)


def format_comparison(traces, queries, limit=10):
    matches = []
    for query in queries:
        match = next((record for record in reversed(traces)
                      if record["query"] == query and not record.get("queryCaptureTruncated", False)), None)
        if match is None:
            raise DiagnosticError("no complete trace for exact query " + _quoted(query))
        matches.append(match)
    left, right = matches
    lines = ["Query comparison: " + _quoted(queries[0]) + " vs " + _quoted(queries[1])]
    if left["model"]["id"] != right["model"]["id"]:
        lines.append("Text-vector cosine: not comparable (model IDs differ)")
    else:
        try:
            cosine = cosine_similarity(vector_values(left.get("queryVector")), vector_values(right.get("queryVector")))
            lines.append("Text-vector cosine: " + _format_number(cosine))
        except DiagnosticError as error:
            lines.append("Text-vector cosine: n/a (" + str(error) + ")")
    left_ids = set(left["displayedIds"][:limit])
    right_ids = set(right["displayedIds"][:limit])
    common = left_ids & right_ids
    union = left_ids | right_ids
    jaccard = len(common) / len(union) if union else None
    lines.append("Displayed overlap@" + str(limit) + ": common=" + str(len(common))
                 + " left=" + str(len(left_ids)) + " right=" + str(len(right_ids))
                 + " jaccard=" + _format_number(jaccard))
    if left["minSimilarity"] != right["minSimilarity"]:
        lines.append("Similarity filters differ between these requests.")
    if left["index"] != right["index"]:
        lines.append("Index metadata differs between these requests.")
    lines.append("Vector distance and result overlap alone do not establish semantic or language accuracy.")
    return "\n".join(lines)


def _positive(value):
    try:
        number = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("must be a positive integer") from error
    if number <= 0:
        raise argparse.ArgumentTypeError("must be a positive integer")
    return number


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="exported ZIP or directory containing trace JSON")
    parser.add_argument("--last", type=_positive, default=5, help="show the latest N traces (default: 5)")
    parser.add_argument("--limit", type=_positive, default=10, help="show up to N displayed images per trace (default: 10)")
    parser.add_argument("--compare", nargs=2, metavar=("QUERY_A", "QUERY_B"), help="compare latest traces for two exact query strings")
    parser.add_argument("--media-id", type=int, help="also inspect this image ID in each shown trace")
    args = parser.parse_args(argv)
    if args.media_id is not None and args.media_id < 0:
        parser.error("--media-id must be nonnegative")
    try:
        traces = load_traces(args.source)
        for trace in traces[-args.last:]:
            print(format_trace(trace, args.limit, args.media_id))
            print()
        if args.compare:
            print(format_comparison(traces, args.compare, args.limit))
        return 0
    except (DiagnosticError, OSError, zipfile.BadZipFile, RuntimeError, OverflowError) as error:
        print("Cannot analyze diagnostics: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
