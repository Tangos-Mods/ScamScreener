#!/usr/bin/env python3
"""
Train a tiny local scam classifier and export weights for scam-screener-local-ai-model.json.

Usage:
  python scripts/train_local_ai.py --data scripts/training_data.csv --out model.json

CSV columns (required):
  message,label,pushes_external_platform,demands_upfront_payment,requests_sensitive_data,claims_middleman_without_proof,repeated_contact_attempts

label must be 0 or 1.
"""

from __future__ import annotations

import argparse
import csv
import json
from dataclasses import dataclass
from pathlib import Path

from sklearn.linear_model import LogisticRegression


PAYMENT_WORDS = ("pay", "payment", "vorkasse", "coins", "money", "btc", "crypto")
ACCOUNT_WORDS = ("password", "passwort", "2fa", "code", "email", "login")
URGENCY_WORDS = ("now", "quick", "fast", "urgent", "sofort", "jetzt")
TRUST_WORDS = ("trust", "legit", "safe", "trusted", "middleman")
TOO_GOOD_WORDS = ("free", "100%", "guaranteed", "garantiert", "dupe", "rank")
PLATFORM_WORDS = ("discord", "telegram", "t.me", "server", "dm")

FEATURE_NAMES = (
    "hasPaymentWords",
    "hasAccountWords",
    "hasUrgencyWords",
    "hasTrustWords",
    "hasTooGoodWords",
    "hasPlatformWords",
    "hasLink",
    "hasSuspiciousPunctuation",
    "ctxPushesExternalPlatform",
    "ctxDemandsUpfrontPayment",
    "ctxRequestsSensitiveData",
    "ctxClaimsMiddlemanWithoutProof",
    "ctxRepeatedContact3Plus",
)


def has_any(text: str, words: tuple[str, ...]) -> float:
    return 1.0 if any(w in text for w in words) else 0.0


def features(row: dict[str, str]) -> list[float]:
    msg = (row.get("message") or "").lower()
    repeated = int(row.get("repeated_contact_attempts") or 0)
    return [
        has_any(msg, PAYMENT_WORDS),
        has_any(msg, ACCOUNT_WORDS),
        has_any(msg, URGENCY_WORDS),
        has_any(msg, TRUST_WORDS),
        has_any(msg, TOO_GOOD_WORDS),
        has_any(msg, PLATFORM_WORDS),
        1.0 if ("http://" in msg or "https://" in msg or "www." in msg) else 0.0,
        1.0 if ("!!!" in msg or "??" in msg or "$$" in msg) else 0.0,
        float(int(row.get("pushes_external_platform") or 0)),
        float(int(row.get("demands_upfront_payment") or 0)),
        float(int(row.get("requests_sensitive_data") or 0)),
        float(int(row.get("claims_middleman_without_proof") or 0)),
        1.0 if repeated >= 3 else 0.0,
    ]


def train(rows: list[dict[str, str]]) -> dict:
    x = [features(r) for r in rows]
    y = [int(r["label"]) for r in rows]

    model = LogisticRegression(max_iter=2000)
    model.fit(x, y)

    weights = model.coef_[0]
    out = {"version": 1, "intercept": float(model.intercept_[0])}
    for name, w in zip(FEATURE_NAMES, weights):
        out[name] = float(w)
    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    with args.data.open("r", encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise SystemExit("No rows found in training data.")

    model = train(rows)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(model, indent=2), encoding="utf-8")
    print(f"Wrote model to {args.out}")


if __name__ == "__main__":
    main()
