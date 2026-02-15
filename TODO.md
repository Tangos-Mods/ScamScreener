# TODO's

## Overhaul der Action Buttons in den Risk Messages
- Entferne die Buttons scam und legit in den Riskmessages
- stattdessen erstelle einen neuen button manage (Hover: öffnet ein fenster zum auswerten und der anschließenden option die Daten hochzuladen)
- der Upload Button öffnet ein Screen, bei dem alle Nachrichten des Spielers der geflagged wurde sichtbar sind
- Klickt man eine Nachricht wird sie hellrot und danach hellgrün, beim dritten mal wieder weiß
  - Hellrot markiert eine scam zeile
  - hellgrün markiert eine legit zeile
  - weiß wird ignoriert
- Im screen gibt es dann drei buttons
  - cancel: schließt den Screen ohne zu speichern
  - save: speichert die daten in die trainings csv -> und durchläuft die bereits implementierte normalisierung
  - save & upload: speichert die daten in die trainings csv und lädt sie über den Webhook hoch -> und durchläuft auch die bereits implementierte normalisierung
Das neue Alert Design sollte dann in etwa so aussehen:

===============================
        HIGH RISK MESSAGE
        Kd_Gaming1 | 45
        [manage] [info]
===============================

- info öffnet dann auch ein Screen, welches die Aktuelle Rules zeile (Similarity Match, External Platform Push, etc.) genauer aufschlüsselt.

nach dem risk alert kommt eine separate nachricht mit einer kleinen education:
  [ScamScreener] The user is trying to move you over to an external platform. Scammers often do this, so proceed with caution. If you're unsure whether it's a scam, treat it as one until proven otherwise. More info and help can be found here. [disable info message]
diese message kann dann über den click deaktiviert werden. Dafür erstelle einen neue config namens scamscreener-edu.json wo festgehalten wird, welche dieser Edu Messages deakiviert ist.
Die Edu Messages sollen in einer Separaten Klasse erfasst werden


## Alert Threshold Change
- ändere den default Alert Threshold auf Medium ab und erzwinge diese einstellung einmalig, sodass Spieler die die Mod schon haben, auf diese einstellung wechseln. Aber das darf nur ein einziges mal geändert werden. Danach ist der Force wieder weg, sodass die Spielerpräferenz wieder persistent bleibt