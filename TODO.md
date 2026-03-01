# TODO

## Kritisch

- [ ] `Stage Logic / Rule`: Die erste echte Erkennungslogik in `RuleStage` einbauen und v1-Regeln kontrolliert portieren.
- [ ] `Stage Logic / Levenshtein`: Fuzzy-Matching und Textnormalisierung in `LevenshteinStage` umsetzen.
- [ ] `Stage Logic / Mute`: Echte Mute-/Ignore-Logik in `MuteStage` einbauen, damit System-/irrelevante Nachrichten frueh sauber rausfallen.
- [ ] `Review Workflow`: Die Review-Queue um Suche, Filter und echte Bearbeitungsaktionen erweitern.

## Hoch

- [ ] `Behavior Stage`: Zustandsmodell fuer wiederholte Kontakte, Frequenz und Senderverhalten aufbauen.
- [ ] `Trend Stage`: Globalen Kurzzeit-Speicher fuer wiederkehrende Muster ueber mehrere Nachrichten hinweg einfuehren.
- [ ] `Funnel Stage`: Sequenz-/Kontextspeicher fuer mehrstufige Risk-Verlaeufe bauen.
- [ ] `Review Persistence`: Review-Eintraege optional auf Platte sichern, damit die Queue Neustarts ueberlebt.
- [ ] `Player List Actions`: Direkte GUI- oder Command-Aktionen fuer Add/Remove in Whitelist und Blacklist ausbauen.

## Mittel

- [ ] `Settings Screens`: Weitere Settings-Screens fuer Stage-, Debug- und spaeter Regelkonfiguration auf der neuen GUI-Basis bauen.
- [ ] `Review Screen`: Aktionen wie "zur Blacklist hinzufuegen", "entfernen", "Verdict speichern" direkt aus dem Review-Screen ergaenzen.
- [ ] `Telemetry / Debug`: Debug-Ausgaben, Decision-Gruende und optional Stage-Trace fuer Diagnose verfuegbar machen.
- [ ] `Tests / Pipeline`: Unit-Tests pro echter Stage und Integrations-Tests fuer reale Pipeline-Flows ergaenzen.
- [ ] `Tests / GUI and Commands`: Grundlegende Tests fuer Review-Store, lokale Commands und Config-Mutationen ergaenzen.

## Niedriger

- [ ] `Model Stage`: Modell-Anbindung, Feature-Aufbereitung und Fallback-Verhalten definieren.
- [ ] `Preprocessing`: Zusaetzliche Chat-Kontext-Aufbereitung wie Parser, Trigger-Kontext oder Intent-Tags ergaenzen.
- [ ] `ModMenu / GUI Polish`: GUI-Navigation, Labels und Datenfluss zwischen Screens weiter auspolieren.
- [ ] `API / Extensions`: Oeffentliche Hooks fuer weitere Pipeline-Erweiterungen und feinere Runtime-Events nachziehen.
- [ ] `Documentation`: Dokumentation der Pipeline-Slots, GUI-Flows, API-Vertraege und Konfigurationsstruktur aktualisieren.
