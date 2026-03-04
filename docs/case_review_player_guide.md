# Case Review Guide (Spieler)

## Ziel
Im Case Review ordnest du Nachrichten in einem Fall ein:

- gehoert zur Story (`Context`)
- ist ein echtes Scam-Signal (`Signal`)
- soll ignoriert werden (`Exclude`)

Danach speicherst du den Fall als `Safe` oder `Risk`.
Diese Daten werden spaeter vom Dev im IDE fuer Training/Tuning genutzt.

## Review oeffnen

- UI: `ScamScreener Settings` -> `Case Review`
- Command: `/scamscreener review`

## Ein Case bearbeiten

1. Fall aus der Queue waehlen oder `New Case` starten.
2. `Review Case` oeffnen.
3. Pro Nachricht Rolle setzen:
   - `Exclude`: nicht relevant
   - `Context`: relevant, aber kein direktes Signal
   - `Signal`: klares Signal fuer Scam-Bewertung
4. Bei `Signal` optional Tags setzen (z. B. external platform, payment).
5. Optional `Advanced`-Zuordnungen aktivieren:
   - hier markierst du feste Stage-Hinweise (`stage.*::reason.*`)
6. Fall speichern:
   - `Save Risk` fuer Scam-Fall
   - `Save Safe` fuer legitimen Fall
   - `Dismiss` fuer bewusst ignorierte Faelle

## Wichtig fuer gute Datenqualitaet

- Mindestens eine Nachricht als `Signal`, wenn du `Save Risk` nutzt.
- Context-Nachrichten lieber drin lassen als loeschen, wenn sie den Ablauf erklaeren.
- Advanced-Mapping nur setzen, wenn du sicher bist, welche Stage wirklich passt.

## Export fuer den Dev

- Button: `Export for Dev` (im Review-Screen oder Main-Screen)
- Command: `/scamscreener review export`

Es wird genau eine Datei geschrieben:

- `training-cases-v2.jsonl`

Diese Datei schickst du an den Dev.

## Was wird exportiert?

- Nur Cases mit Verdict `Risk` oder `Safe`
- Rollen/Tags/Mapping-IDs
- Beobachtete Pipeline-Stage-Ergebnisse mit stabilen IDs

## Privacy-Hinweise

- Sender-UUID wird nicht in Review-/Trainingspayload persistiert.
- UUID-Persistenz bleibt nur fuer `whitelist.json` und `blacklist.json`.
- Namen koennen im Nachrichtentext enthalten sein und werden nicht teuer nachtraeglich aufgeloest.

## Typische Fehler

- Fall als `Risk` gespeichert, aber kein `Signal` gesetzt -> wird abgelehnt.
- Nur Trigger-Nachricht markiert, kein Context -> oft schlechtere Trainingsqualitaet.
- Advanced-Mapping mit Freitext begruendet statt stabiler IDs -> vermeiden.
