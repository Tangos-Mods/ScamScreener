#!/usr/bin/env python3
"""
Train the v9 local model from training-data CSV and export
scripts/scam-screener-local-ai-model.json format.

Usage:
  python scripts/train_local_ai.py --data scripts/scam-screener-training-data.csv --out scripts/scam-screener-local-ai-model.json
  python scripts/train_local_ai.py --data scripts/scam-screener-training-data.csv --out scripts/scam-screener-local-ai-model.json -funnel
  python scripts/train_local_ai.py --data scripts/scam-screener-training-data.csv --out scripts/scam-screener-local-ai-model.json -ai
  python scripts/train_local_ai.py --data scripts/scam-screener-training-data.csv --out scripts/scam-screener-local-ai-model.json -tokens
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import re
from pathlib import Path

from sklearn.linear_model import LogisticRegression


DENSE_FEATURE_NAMES = [
    "kw_payment",
    "kw_account",
    "kw_urgency",
    "kw_trust",
    "kw_too_good",
    "kw_platform",
    "has_link",
    "has_suspicious_punctuation",
    "ctx_pushes_external_platform",
    "ctx_demands_upfront_payment",
    "ctx_requests_sensitive_data",
    "ctx_claims_middleman_without_proof",
    "ctx_too_good_to_be_true",
    "ctx_repeated_contact_3plus",
    "ctx_is_spam",
    "ctx_asks_for_stuff",
    "ctx_advertising",
    "intent_offer",
    "intent_rep",
    "intent_redirect",
    "intent_instruction",
    "intent_payment",
    "intent_anchor",
    "funnel_step_norm",
    "funnel_sequence_norm",
    "funnel_full_chain",
    "funnel_partial_chain",
    "rapid_followup",
    "channel_pm",
    "channel_party",
    "channel_public",
    "rule_hits_norm",
    "similarity_hits_norm",
    "behavior_hits_norm",
    "trend_hits_norm",
    "funnel_hits_norm",
]

FUNNEL_DENSE_FEATURE_NAMES = [
    "ctx_pushes_external_platform",
    "ctx_repeated_contact_3plus",
    "intent_offer",
    "intent_rep",
    "intent_redirect",
    "intent_instruction",
    "intent_payment",
    "intent_anchor",
    "funnel_step_norm",
    "funnel_sequence_norm",
    "funnel_full_chain",
    "funnel_partial_chain",
    "rapid_followup",
    "funnel_hits_norm",
]
FUNNEL_DENSE_INDEXES = [DENSE_FEATURE_NAMES.index(name) for name in FUNNEL_DENSE_FEATURE_NAMES]

PAYMENT_WORDS = ("pay", "payment", "vorkasse", "coins", "money", "btc", "crypto")
ACCOUNT_WORDS = ("password", "passwort", "2fa", "code", "email", "login")
URGENCY_WORDS = ("now", "quick", "fast", "urgent", "sofort", "jetzt")
TRUST_WORDS = ("trust", "legit", "safe", "trusted", "middleman")
TOO_GOOD_WORDS = ("free", "100%", "guaranteed", "garantiert", "dupe", "rank")
PLATFORM_WORDS = ("discord", "telegram", "t.me", "server", "dm", "vc", "voice")
TOKEN_PATTERN = re.compile(r"[a-z0-9_]{3,24}")
TOKEN_MIN_NGRAM = 3
TOKEN_MAX_NGRAM = 5
MAX_TOKEN_FEATURES = 5000
MIN_TOKEN_COUNT = 3


def _bool(v: str) -> float:
    if v is None:
        return 0.0
    t = v.strip().lower()
    if not t:
        return 0.0
    if t in ("true", "yes"):
        return 1.0
    try:
        return 1.0 if int(t) > 0 else 0.0
    except ValueError:
        return 0.0


def _int(v: str, fallback: int = 0) -> int:
    try:
        return int((v or "").strip())
    except ValueError:
        return fallback


def _float(v: str, fallback: float = 0.0) -> float:
    try:
        return float((v or "").strip())
    except ValueError:
        return fallback


def _has_any(msg: str, words: tuple[str, ...]) -> float:
    return 1.0 if any(w in msg for w in words) else 0.0


def _norm(value: float, cap: float) -> float:
    if cap <= 0:
        return 0.0
    out = value / cap
    if out < 0:
        return 0.0
    if out > 1:
        return 1.0
    return out


def _tokenize_words(text: str) -> list[str]:
    if not text:
        return []
    return TOKEN_PATTERN.findall(text.lower())


def _extract_token_features(text: str) -> set[str]:
    words = _tokenize_words(text)
    if not words:
        return set()

    features: set[str] = set()
    for i in range(len(words)):
        for n in range(TOKEN_MIN_NGRAM, TOKEN_MAX_NGRAM + 1):
            end = i + n
            if end > len(words):
                break
            features.add(f"ng{n}:{' '.join(words[i:end])}")
    return features


def _build_token_vocab(messages: list[str], labels: list[int]) -> list[str]:
    if not messages:
        return []

    counts: dict[str, int] = {}
    positives: dict[str, int] = {}
    base_rate = sum(labels) / float(len(labels))

    for message, label in zip(messages, labels):
        for token in _extract_token_features(message):
            counts[token] = counts.get(token, 0) + 1
            if label == 1:
                positives[token] = positives.get(token, 0) + 1

    for min_count in (MIN_TOKEN_COUNT, 2, 1):
        ranked: list[tuple[float, int, str]] = []
        for token, count in counts.items():
            if count < min_count:
                continue
            token_rate = positives.get(token, 0) / float(count)
            score = abs(token_rate - base_rate) * math.log1p(count)
            if score > 0.0:
                ranked.append((score, count, token))

        if ranked:
            ranked.sort(key=lambda item: (-item[0], -item[1], item[2]))
            return [token for _, _, token in ranked[:MAX_TOKEN_FEATURES]]

    return []


def _features(row: dict[str, str]) -> list[float]:
    msg = (row.get("message") or "").lower()
    delta_ms = _float(row.get("delta_ms"), 0.0)
    channel = (row.get("channel") or "unknown").strip().lower()
    repeated = _int(row.get("repeated_contact_attempts"), 0)

    rapid_followup = 0.0 if delta_ms <= 0 else 1.0 - _norm(delta_ms, 120000.0)

    return [
        _has_any(msg, PAYMENT_WORDS),
        _has_any(msg, ACCOUNT_WORDS),
        _has_any(msg, URGENCY_WORDS),
        _has_any(msg, TRUST_WORDS),
        _has_any(msg, TOO_GOOD_WORDS),
        _has_any(msg, PLATFORM_WORDS),
        1.0 if ("http://" in msg or "https://" in msg or "www." in msg) else 0.0,
        1.0 if ("!!!" in msg or "??" in msg or "$$" in msg) else 0.0,
        _bool(row.get("pushes_external_platform")),
        _bool(row.get("demands_upfront_payment")),
        _bool(row.get("requests_sensitive_data")),
        _bool(row.get("claims_middleman_without_proof")),
        _bool(row.get("too_good_to_be_true")),
        1.0 if repeated >= 3 else 0.0,
        _bool(row.get("is_spam")),
        _bool(row.get("asks_for_stuff")),
        _bool(row.get("advertising")),
        _bool(row.get("intent_offer")),
        _bool(row.get("intent_rep")),
        _bool(row.get("intent_redirect")),
        _bool(row.get("intent_instruction")),
        _bool(row.get("intent_payment")),
        _bool(row.get("intent_anchor")),
        _norm(_float(row.get("funnel_step_index"), 0.0), 4.0),
        _norm(_float(row.get("funnel_sequence_score"), 0.0), 40.0),
        _bool(row.get("funnel_full_chain")),
        _bool(row.get("funnel_partial_chain")),
        rapid_followup,
        1.0 if channel == "pm" else 0.0,
        1.0 if channel == "party" else 0.0,
        1.0 if channel == "public" else 0.0,
        _norm(_float(row.get("rule_hits"), 0.0), 3.0),
        _norm(_float(row.get("similarity_hits"), 0.0), 2.0),
        _norm(_float(row.get("behavior_hits"), 0.0), 3.0),
        _norm(_float(row.get("trend_hits"), 0.0), 2.0),
        _norm(_float(row.get("funnel_hits"), 0.0), 2.0),
    ]


def _funnel_label(row: dict[str, str]) -> int:
    explicit = row.get("funnel_label")
    if explicit is not None and str(explicit).strip() != "":
        return 1 if _bool(str(explicit)) > 0.0 else 0

    if _bool(row.get("funnel_full_chain")) > 0.0 or _bool(row.get("funnel_partial_chain")) > 0.0:
        return 1
    if _float(row.get("funnel_step_index"), 0.0) > 0.0:
        return 1
    if _float(row.get("funnel_sequence_score"), 0.0) > 0.0:
        return 1
    if _float(row.get("funnel_hits"), 0.0) > 0.0:
        return 1
    return 0


def _default_model_payload() -> dict:
    return {
        "version": 9,
        "intercept": -2.25,
        "denseFeatureWeights": {name: 0.0 for name in DENSE_FEATURE_NAMES},
        "tokenWeights": {},
        "funnelHead": {
            "intercept": -2.25,
            "denseFeatureWeights": {name: 0.0 for name in FUNNEL_DENSE_FEATURE_NAMES},
        },
    }


def _load_output_model(path: Path) -> dict:
    defaults = _default_model_payload()
    if not path.exists():
        return defaults

    try:
        loaded = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return defaults
    if not isinstance(loaded, dict):
        return defaults

    model = dict(loaded)

    version = model.get("version")
    model["version"] = int(version) if isinstance(version, (int, float)) else defaults["version"]

    intercept = model.get("intercept")
    model["intercept"] = float(intercept) if isinstance(intercept, (int, float)) else defaults["intercept"]

    dense_weights = model.get("denseFeatureWeights")
    dense_out: dict[str, float] = {}
    if isinstance(dense_weights, dict):
        for key, value in dense_weights.items():
            if isinstance(key, str) and isinstance(value, (int, float)):
                dense_out[key] = float(value)
    for name, value in defaults["denseFeatureWeights"].items():
        dense_out.setdefault(name, value)
    model["denseFeatureWeights"] = dense_out

    token_weights = model.get("tokenWeights")
    token_out: dict[str, float] = {}
    if isinstance(token_weights, dict):
        for key, value in token_weights.items():
            if isinstance(key, str) and isinstance(value, (int, float)):
                token_out[key] = float(value)
    model["tokenWeights"] = token_out

    funnel_head = model.get("funnelHead")
    if not isinstance(funnel_head, dict):
        funnel_head = {}
    funnel_intercept = funnel_head.get("intercept")
    funnel_intercept_out = (
        float(funnel_intercept) if isinstance(funnel_intercept, (int, float)) else defaults["funnelHead"]["intercept"]
    )

    funnel_dense_weights = funnel_head.get("denseFeatureWeights")
    funnel_dense_out: dict[str, float] = {}
    if isinstance(funnel_dense_weights, dict):
        for key, value in funnel_dense_weights.items():
            if isinstance(key, str) and isinstance(value, (int, float)):
                funnel_dense_out[key] = float(value)
    for name, value in defaults["funnelHead"]["denseFeatureWeights"].items():
        funnel_dense_out.setdefault(name, value)

    model["funnelHead"] = {
        "intercept": funnel_intercept_out,
        "denseFeatureWeights": funnel_dense_out,
    }
    return model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument("-funnel", "--funnel", action="store_true", help="Train only the funnel head.")
    mode_group.add_argument("-ai", "--ai", action="store_true", help="Train only AI dense weights and intercept.")
    mode_group.add_argument("-tokens", "--tokens", action="store_true", help="Train only token weights.")
    args = parser.parse_args()

    train_all = not (args.funnel or args.ai or args.tokens)
    train_funnel = args.funnel or train_all
    train_ai = args.ai or train_all
    train_tokens = args.tokens or train_all
    fit_main_model = train_ai or train_tokens

    with args.data.open("r", encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))

    if not rows:
        raise SystemExit("No rows found in training data.")

    x: list[list[float]] = []
    messages: list[str] = []
    y: list[int] = []
    funnel_y: list[int] = []
    sample_weight: list[float] = []

    for row in rows:
        label_raw = (row.get("label") or "").strip()
        if label_raw not in ("0", "1"):
            continue
        message = row.get("message") or ""
        x.append(_features(row))
        messages.append(message)
        y.append(int(label_raw))
        funnel_y.append(_funnel_label(row))
        sample_weight.append(_float(row.get("sample_weight"), 1.0))

    if len(x) < 12:
        raise SystemExit("Not enough usable rows (need at least 12).")
    if fit_main_model and not (0 in y and 1 in y):
        raise SystemExit("AI/token training data must contain both labels 0 and 1.")

    funnel_target = funnel_y if (0 in funnel_y and 1 in funnel_y) else y
    if train_funnel and not (0 in funnel_target and 1 in funnel_target):
        raise SystemExit("Funnel training data must contain both labels 0 and 1.")

    out = _load_output_model(args.out)

    if fit_main_model:
        x_main = [row.copy() for row in x]
        token_vocab: list[str] = []
        if train_tokens:
            token_vocab = _build_token_vocab(messages, y)
            token_index = {token: idx for idx, token in enumerate(token_vocab)}
            if token_index:
                token_count = len(token_vocab)
                for idx, message in enumerate(messages):
                    token_values = [0.0] * token_count
                    for token in _extract_token_features(message):
                        token_idx = token_index.get(token)
                        if token_idx is not None:
                            token_values[token_idx] = 1.0
                    x_main[idx].extend(token_values)

        model = LogisticRegression(max_iter=2500)
        model.fit(x_main, y, sample_weight=sample_weight)
        dense_count = len(DENSE_FEATURE_NAMES)

        if train_ai:
            dense_weights = {
                name: float(weight) for name, weight in zip(DENSE_FEATURE_NAMES, model.coef_[0][:dense_count])
            }
            dense_out = dict(out.get("denseFeatureWeights", {}))
            dense_out.update(dense_weights)
            out["intercept"] = float(model.intercept_[0])
            out["denseFeatureWeights"] = dense_out

        if train_tokens:
            token_weights = {
                token: float(weight)
                for token, weight in zip(token_vocab, model.coef_[0][dense_count:dense_count + len(token_vocab)])
            }
            out["tokenWeights"] = token_weights

    if train_funnel:
        funnel_x = [[row[idx] for idx in FUNNEL_DENSE_INDEXES] for row in x]
        funnel_model = LogisticRegression(max_iter=2500)
        funnel_model.fit(funnel_x, funnel_target, sample_weight=sample_weight)
        funnel_dense_weights = {
            name: float(weight) for name, weight in zip(FUNNEL_DENSE_FEATURE_NAMES, funnel_model.coef_[0])
        }
        funnel_out = out.get("funnelHead")
        if not isinstance(funnel_out, dict):
            funnel_out = {}
        funnel_dense_out = funnel_out.get("denseFeatureWeights")
        if not isinstance(funnel_dense_out, dict):
            funnel_dense_out = {}
        funnel_dense_out.update(funnel_dense_weights)
        funnel_out["intercept"] = float(funnel_model.intercept_[0])
        funnel_out["denseFeatureWeights"] = funnel_dense_out
        out["funnelHead"] = funnel_out

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2), encoding="utf-8")
    print(f"Wrote model to {args.out}")


if __name__ == "__main__":
    main()
