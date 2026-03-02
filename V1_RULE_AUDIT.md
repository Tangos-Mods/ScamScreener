# v1 Rule Audit

`OK` = fachlich direkt brauchbar, nur sauber in v2 einhängen  
`REWRITE` = fachlich behalten, aber technisch in v2 neu schneiden  
`RAUS` = nicht direkt übernehmen

Hinweis: `Mute`, `Whitelist` und `Blacklist` sind hier bewusst nicht gelistet, weil sie in v1 Pipeline-Gates waren und keine `ScamRule`s.

| v1-Regel | Hauptquelle in v1 | Spaghetti-Grad | Einschätzung | Status | Begründung |
|---|---|---:|---|---|---|
| `SUSPICIOUS_LINK` | `RuleSignalStage` | Niedrig | Klare, direkte Regex-Regel | `OK` | Fachlich stabil und in v2 schon sauber als deterministische Regel abbildbar. |
| `PRESSURE_AND_URGENCY` | `RuleSignalStage` | Mittel | Gute Heuristik, aber mit vielen Kontext- und Allowlist-Branches in einer Methode | `REWRITE` | Behalten, aber sauberer in v2 mit expliziten Kontext-Gates, Allowlists und getrennten Reason-Codes. |
| `UPFRONT_PAYMENT` | `RuleSignalStage`, `BehaviorSignalStage` | Mittel | Gute Kernregel, aber in v1 teils doppelt über Rule und Behavior modelliert | `REWRITE` | Fachlich wichtig, aber v2 sollte klarer zwischen Textsignal und Verhalten unterscheiden. |
| `ACCOUNT_DATA_REQUEST` | `RuleSignalStage`, `BehaviorSignalStage` | Mittel | Sehr starke Kernregel, aber ebenfalls doppelt über mehrere Pfade verteilt | `REWRITE` | Behalten, aber als klare primäre Regel plus optionale Behavior-Verstärkung. |
| `EXTERNAL_PLATFORM_PUSH` | `BehaviorSignalStage`, implizit auch in Rule-Kontext | Hoch | Inhaltlich wichtig, aber in v1 über Pattern, Behavior und Funnel mehrfach verflochten | `REWRITE` | In v2 als klarer Mix aus `RuleStage` + `FunnelStage`, ohne doppelte Semantik. |
| `DISCORD_HANDLE` | `RuleSignalStage` | Niedrig | Saubere Spezialregel mit klarer Aussage | `OK` | Fachlich präzise und gut portierbar, solange die Gewichte sauber konfiguriert bleiben. |
| `FAKE_MIDDLEMAN_CLAIM` | `BehaviorSignalStage`, `BehaviorAnalyzer` | Mittel | Sinnvoll, aber in v1 eng an Pattern-Flags und Folgeverhalten gekoppelt | `REWRITE` | Behalten, aber in v2 besser als klare Middleman-/Trust-Unterregel statt verstreuter Flag-Pfad. |
| `TOO_GOOD_TO_BE_TRUE` | `RuleSignalStage` | Niedrig | Klassische, klar definierte Phrase-Regel | `OK` | Einfach, effektiv und direkt portierbar. |
| `TRUST_MANIPULATION` | `RuleSignalStage` | Mittel | Gute Heuristik, aber stark über Phrase-Score und angrenzende Middleman-Semantik überladen | `REWRITE` | Behalten, aber v2 sollte `trust`, `middleman`, `reputation bait` sauberer auseinanderziehen. |
| `SPAMMY_CONTACT_PATTERN` | `BehaviorAnalyzer`, `BehaviorSignalStage` | Mittel | Gute Verhaltensregel, aber v1 war sehr streak-basiert und relativ grob | `REWRITE` | In v2 besser über echten Zeitfenster-Store und mehrere Burst-/Repeat-Signale. |
| `MULTI_MESSAGE_PATTERN` | `TrendStore`, `TrendSignalStage` | Hoch | Fachlich sinnvoll, aber stark von internem TrendStore und vorhandenen Signalen abhängig | `REWRITE` | Behalten, aber in v2 als transparente Cross-Sender-/Wave-Heuristik ohne verdeckte Signalabhängigkeit. |
| `FUNNEL_SEQUENCE_PATTERN` | `IntentTagger`, `FunnelStore`, `FunnelSignalStage` | Hoch | Sehr nützlich, aber in v1 einer der komplexesten und am stärksten verketteten Pfade | `REWRITE` | Behalten, aber in v2 als kleinere, explizite Sequenzschritte statt schwer nachvollziehbarer Kombinatorik. |
| `SIMILARITY_MATCH` | `LevenshteinSignalStage` | Hoch | Inhaltlich stark, aber mit Datei-I/O, Training-Cache, Rule-Phrasen und Schwellen in einer Klasse | `REWRITE` | Behalten, aber in v2 in saubere Blöcke trennen: feste Phrasen, Trainingsdaten, Threshold-Logik. |
| `LOCAL_AI_RISK_SIGNAL` | `AiSignalStage` | Mittel | Sinnvoll als spätes Zusatzsignal, aber in v1 eng an globale `ScamRules` gekoppelt | `REWRITE` | In v2 erst ganz am Schluss als modularer Hook, nicht als früh verklebter Sonderpfad. |
| `LOCAL_AI_FUNNEL_SIGNAL` | `AiSignalStage` | Mittel | Nützlich, aber fachlich zu speziell, um die Stage-Struktur zu diktieren | `REWRITE` | In v2 nur als optionaler später Zusatz zum Graubereich. |
| `ENTROPY_BONUS` (interne Heuristik, keine offizielle `ScamRule`) | `RuleSignalStage` | Hoch | Versteckte Sonderlogik mit ungewöhnlicher Kopplung an `entropyBonusWeight()` | `RAUS` | So nicht übernehmen. Wenn überhaupt, dann nur später als klar benannte, separat konfigurierbare Low-Confidence-Heuristik. |

## Kurzfazit

- Direkt fachlich brauchbar: `SUSPICIOUS_LINK`, `DISCORD_HANDLE`, `TOO_GOOD_TO_BE_TRUE`
- Auf jeden Fall behalten, aber technisch neu schneiden: fast alle komplexeren Regeln
- Nicht direkt übernehmen: `ENTROPY_BONUS` in seiner v1-Form

## Empfohlene Portier-Reihenfolge

1. `RuleStage`: verbleibende Regelbreite aus `PRESSURE_AND_URGENCY`, `UPFRONT_PAYMENT`, `ACCOUNT_DATA_REQUEST`, `TRUST_MANIPULATION`
2. `LevenshteinStage`: `SIMILARITY_MATCH` sauber verbreitern
3. `BehaviorStage`: `SPAMMY_CONTACT_PATTERN`, `FAKE_MIDDLEMAN_CLAIM`, ergänzende Behavior-Signale
4. `TrendStage`: `MULTI_MESSAGE_PATTERN`
5. `FunnelStage`: `FUNNEL_SEQUENCE_PATTERN`
6. `ModelStage`: spätere AI-Signale (`LOCAL_AI_*`)
