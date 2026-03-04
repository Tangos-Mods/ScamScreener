# TODO

## Status Update (2026-03-04)

- [x] `training_case_v2` is the canonical export format (replaces the old `training_case_v1` idea).
- [x] Case review now has a real export flow (`review export` command and UI button).
- [x] `ContextStage` is integrated into the default pipeline.
- [x] Stage and reason mapping uses stable IDs (`stage.*`, `reasonId`, `mappingId`).
- [x] Legacy preview command path was removed.
- [x] Blacklist and whitelist keep UUID persistence in their dedicated config files.
- [x] Persisted review/training payloads strip sender UUID identity; message text may still contain player names by design.

## Zielbild für eine saubere v2

ScamScreener v2 soll am Ende klar in zwei Ebenen getrennt sein:

- Eine neue, saubere Pipeline mit stabilen Stages, stabilen Signal-IDs und später einer echten `ContextStage`.
- Eine spielerfreundliche Oberfläche, die nicht mehr zwischen alten v1-Resten, Preview-Flows und neuen Trainingsideen mischt.

Der v1-Spielerflow wurde bereits weitgehend wiederhergestellt. Die größten offenen Punkte sind jetzt Datenmodell, Export, Privacy und das Fertigziehen der echten v2-Bausteine.

## Für eine saubere v2 noch offen

### 1. Core / Pipeline

- [ ] `ContextStage` entwerfen und implementieren.
- [ ] Festlegen, welche Features in die spätere `ContextStage` eingehen und welche nur für Regel-Kalibrierung gedacht sind.
- [ ] Die spätere `ContextStage` als echten Nachfolger des entfernten alten AI-/Model-Features einhängen.
- [ ] Stabile Signal- und Rule-IDs durchgängig verwenden, damit Stages, Review und spätere Exporte nicht auf lesbaren UI-Texten basieren.

### 2. Review / Training Daten

- [ ] Ein kanonisches, anonymes `training_case_v1`-Format definieren.
- [ ] Aus dem Case-Review einen echten Exportpfad bauen statt nur `review.json`.
- [ ] Aus dem kanonischen Format später getrennte Exporte für `ContextStage`-Training und Rule-/Stage-Kalibrierung ableiten.
- [ ] Speichern, welche Nachrichten zum Fall gehören, welche nur Kontext sind und welche echte Signale tragen, ohne dass Entwickler das später erraten müssen.
- [ ] Sicherstellen, dass Advanced-Rule-Mapping nicht auf Anzeige-Strings, sondern auf stabilen maschinenlesbaren IDs basiert.

### 3. Privacy / TOS

- [ ] Alle persistierten Review-/Trainingsdaten von Spielernamen bereinigen.
- [ ] Alle persistierten Review-/Trainingsdaten von UUIDs bereinigen.
- [ ] Prüfen, ob irgendwo noch indirekte Identifikatoren oder rekonstruierbare Personendaten in lokalen Dateien landen.
- [ ] Den finalen Export so gestalten, dass er community-tauglich, nachvollziehbar und trotzdem TOS-sicher ist.

### 4. Produkt / UX

- [ ] Den Case-Review-Flow sprachlich finalisieren, damit Begriffe wie `Review`, `Case`, `Signal`, `Context` und `Dismissed` überall konsistent sind.
- [ ] Den Review-Queue-Flow endgültig auf reine Case-Arbeit ausrichten, ohne doppelte oder halbveraltete Shortcuts.
- [ ] Den Export-Flow im UI sichtbar und logisch machen, statt über alte Namen wie `Review Training CSV`.
- [ ] Die verbleibenden Screens auf einen finalen v2-Wortlaut umstellen, statt weiterhin v1-/Legacy-Hinweise im Beschreibungstext zu zeigen.
- [ ] Den Metrics-Bereich von einem Platzhalter auf echte v2-Observability ausbauen.

### 5. Technische Bereinigung

- [ ] Prüfen, welche Alt-Screens nur noch als Überbleibsel existieren und entfernt werden können.
- [ ] Prüfen, welche Debug-/Preview-Kommandos überhaupt noch im öffentlichen Nutzerflow sichtbar sein sollen.
- [ ] Veraltete Copy, Kommentare und Legacy-Hinweise im Code aufräumen, sobald die finalen v2-Begriffe feststehen.
- [ ] Dokumentation für Pipeline, Review-Cases, Advanced Rules und Exportformat schreiben.

## Noch legacy oder alte Preview-Artefakte

Diese Punkte funktionieren zwar teilweise noch, sind aber noch kein sauberer finaler v2-Zustand:

- [ ] `Review Training CSV` ist noch ein alter Legacy-Name, obwohl dort aktuell kein echter CSV-Trainingsflow dahinter steht.
- [ ] `Upload Training Data` ist weiter sichtbar, aber noch deaktiviert und ohne realen v2-Exportpfad.
- [ ] Der Command `preview` ist weiterhin ein klarer Dev-/Preview-Pfad für künstliche Warnmeldungen.
- [ ] Der Metrics-Screen ist noch eher ein Platzhalter im alten Slot statt eine finale v2-Metrikansicht.
- [ ] Im Debug-Bereich steht weiterhin Legacy-/v1-Wording.
- [ ] Im Runtime-Bereich steht weiterhin Übergangs-Wording zwischen v1-Layout und v2-Funktion.
- [ ] Mehrere Screens sind absichtlich v1-nah gestylt; funktional ist das gewollt, optisch ist es noch kein eigener finaler v2-Look.
- [ ] Das Advanced-Rule-Mapping nutzt noch menschenlesbare Detailtexte statt stabile technische IDs.
- [ ] `ReviewDetailScreen` sollte geprüft werden, da der aktuelle Info-Flow bereits über `AlertInfoScreen` läuft.
- [ ] Teile des Menüs und der Screen-Copy fühlen sich noch wie ein wiederhergestellter v1-Shell-Wrapper um neue v2-Logik an, nicht wie eine endgültig zusammengezogene v2.

## Neue oder geänderte Features

| Feature | Vorher | Jetzt |
| --- | --- | --- |
| Review Queue | Fokus auf einzelne Message-Einträge und direkte Row-Verdicts. | Fokus auf reine Cases als Arbeitsobjekte. |
| Review-Logik | Nachrichten wurden eher einzeln bewertet. | Review ist fallbasiert: ein Case enthält mehrere Nachrichten mit Kontext und Signalen. |
| Case Review | Klassischer Einzelzeilen-Flow näher an altem v1-Review. | `Exclude / Context / Signal`, Signal-Tags, Case-Verdict und optionales Advanced-Mapping. |
| Add Case Message | Manuelle Eingabe war möglich bzw. der Flow war unklar. | Öffnet einen Chat-Picker für gecachte Nachrichten statt Freitext. |
| Case Message Picker | Kein echter Auswahl-Workflow für vorhandene Chatzeilen. | Auswahl aus gecachten Chatnachrichten, mit `Add Selected`, `Add All Visible` und `Add Last 10`. |
| Picker Inhalt | Konnte irrelevante oder systemische Zeilen enthalten. | Zeigt nur echte Player-Nachrichten und aktualisiert sich automatisch. |
| Chat-Erkennung | Zu generisch; viele Zeilen mit Doppelpunkt konnten falsch als Playerchat gelten. | Hypixel-nahe Erkennung: Public nur mit `[Level]`, DMs nur `From:`, Guild nur `Guild >`, Party nur `Party >`. |
| UI-Nachrichtenanzeige | Prefixe und Spielernamen wurden oft mit angezeigt. | In Review-/Alert-Views wird vorrangig der bereinigte Nachrichteninhalt gezeigt. |
| Rules Screen | Normale Nutzer konnten direkt an Score-/Threshold-Werten drehen. | Der normale Rules-Screen schaltet nur noch Stages, Trigger und Combos an oder aus. |
| Advanced Rules | Kein klarer Schutz vor riskanten Pipeline-Änderungen. | Eigener Experten-Screen hinter einer `I KNOW WHAT I AM DOING`-Warnung. |
| Toggle-Darstellung | `ON/OFF` war optisch neutral und uneinheitlich. | `ON` ist hellgrün, `OFF` hellrot und zentral vereinheitlicht. |
| Behavior-Spam-Gewichtung | Mehrere Spam-Signale konnten sich stärker aufsummieren. | Spam-bezogene Behavior-Signale addieren insgesamt maximal `+1`. |
| Sichtbare Risk-Warnung | Konnte schon bei sehr niedrigen Scores sichtbar werden. | Sichtbare Risk-Warnung/Ping greift erst ab `10` Punkten, außer bei `BLOCK`. |
| Alert / Info Buttons | `[manage]` und `[info]` waren zwischenzeitlich nicht mehr im v1-Stil. | `[manage]` und `[info]` sind wieder im v1-nahen Alert-Flow vorhanden. |
| Review `Info` | Konnte auf einem anderen Detailscreen landen als Risk-Message-`[info]`. | Nutzt denselben Info-Screen wie der Alert-`[info]`-Pfad. |
| Altes AI-/Model-Feature | Alter `ModelStage`-Placeholder, AI-Screens und AI-Toggles waren noch vorhanden. | Komplett entfernt; später soll stattdessen eine echte `ContextStage` kommen. |

## Empfohlene Reihenfolge ab jetzt

1. Privacy / TOS bereinigen: keine Spielernamen und keine UUIDs mehr in lokalen Review-/Trainingsdaten.
2. `training_case_v1` als kanonisches, anonymes Datenformat festziehen.
3. Einen echten Export aus dem Case-Review bauen.
4. Das Advanced-Rule-Mapping auf stabile IDs umstellen.
5. Die echte `ContextStage` auf Basis dieses Datenmodells entwerfen.
6. Die verbleibenden Legacy-/Preview-Reste aus UI, Commands und Copy entfernen.
