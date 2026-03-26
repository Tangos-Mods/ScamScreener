# AGENTS.md

## Zweck
Dieses Repository ist für Minecraft-Modding.  
Implementiere nur das, was für die angeforderte Funktion wirklich nötig ist.

## Grundregeln
- Kein Overengineering.
- Keine unnötigen Abstraktionen.
- Keine generischen Framework-artigen Konstrukte, wenn eine einfache direkte Lösung reicht.
- Keine "future-proofing"-Implementierungen ohne konkreten Bedarf.
- Keine toten Helferklassen, Wrapper, Manager, Registry-Layer oder Utility-Sammlungen ohne klaren aktuellen Nutzen.
- Keine künstlich aufgeblähten Architekturen.

## Stilvorgaben
- Schreibe einfachen, direkten, lesbaren Code.
- Bevorzuge konkrete Implementierungen statt unnötiger Verallgemeinerung.
- Halte Klassen klein und zweckgebunden.
- Halte Methoden kurz und klar.
- Verwende sprechende Namen statt unnötiger Kommentare.
- Kommentare nur dort, wo etwas nicht offensichtlich ist.

## Was vermieden werden soll
- AI-typische "Enterprise"-Patterns für kleine Features.
- Übermäßige Nutzung von Interfaces ohne echten Mehrwert.
- Builder, Factory, Service, Provider, Adapter etc., wenn sie nicht wirklich gebraucht werden.
- Defensive Abstraktionen für hypothetische zukünftige Use-Cases.
- Mehrschichtige Architektur für triviale Logik.
- Duplizierte Hilfslogik in "Utils", nur um Code "sauberer" aussehen zu lassen.
- Komplexe Konfigurations- oder Event-Systeme für einfache Abläufe.

## Implementierungsprinzip
Bei jeder Änderung gilt:
1. Was ist die konkrete Anforderung?
2. Was ist die kleinste saubere Lösung?
3. Implementiere genau diese.
4. Nichts darüber hinaus.

## Minecraft-Modding spezifisch
- Nutze Vanilla/Fabric/Forge/NeoForge APIs direkt, wenn möglich.
- Führe keine zusätzliche Abstraktionsschicht ein, nur um APIs zu "entkoppeln", sofern es keinen echten Grund gibt.
- Mixins, Events, Registries und Screens nur so komplex wie nötig.
- HUD-, Render-, GUI- und Networking-Code soll funktional und minimal bleiben.
- Kein künstliches Zerlegen kleiner Features in viele Dateien.

## Bei Refactoring
- Refactore nur bei echtem Nutzen:
    - bessere Lesbarkeit
    - weniger Duplikation
    - klarere Verantwortlichkeiten
    - notwendige technische Korrektur
- Kein Refactoring nur aus Stiltrieb.

## Wenn unsicher
Bevorzuge:
- weniger Code
- weniger Dateien
- weniger Abstraktion
- weniger Magie

## Ziel
Der Code soll wirken, als hätte ihn ein erfahrener Modder pragmatisch und bewusst geschrieben.  
Nicht wie ein generischer AI-Output.
