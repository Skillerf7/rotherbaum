# Rotherbaum

Ein eigener, komplett lokaler/offline Android-Musikplayer im Stil von
Poweramp: echter parametrischer Equalizer, Bibliothek, Playlists,
Favoriten, Bewertungen, Spektrum-Analyzer.

## APK über GitHub bauen (ohne Android Studio)

1. Neues **leeres** GitHub-Repository anlegen (z.B. `rotherbaum`).
2. Den kompletten Inhalt dieses Ordners in das Repo pushen (die
   Ordnerstruktur `.github/`, `app/`, `settings.gradle.kts` usw. muss
   direkt im Repo-Wurzelverzeichnis liegen, nicht in einem
   Unterordner).
3. Im Repo oben auf **Actions** klicken. Der Workflow **"Build APK"**
   startet automatisch beim Push (dauert beim ersten Mal ca. 5-10
   Minuten, weil Android SDK + Gradle erst heruntergeladen werden).
4. Wenn der Workflow-Lauf fertig ist (grüner Haken), draufklicken →
   ganz unten unter **Artifacts** liegt `rotherbaum-debug-apk` zum
   Download (eine ZIP mit der `.apk` drin).
5. APK aufs Handy, **"Installation aus unbekannten Quellen"**
   erlauben, installieren.

Kein lokales Android Studio, kein Android SDK auf dem eigenen PC
nötig - GitHub baut die APK für dich.

## Was ist echt und fertig implementiert

- **Vollwertiger parametrischer EQ**: 10 Bänder (31 Hz - 16 kHz),
  jedes mit frei einstellbarer Frequenz/Gain/Q - eigene
  Biquad-Filter-DSP direkt im Audio-Pfad, kein Android-Bordmittel mit
  fixen Herstellerbändern.
- Preamp, Bass-/Höhen-Shelf, Limiter (weicher Soft-Clip-Limiter)
- Echtzeit-Spektrum-Analyzer (Visualizer-API + eigene FFT)
- EQ-Presets speichern/laden (lokale Datenbank)
- Bibliothek: Alle Titel, Ordner, Alben, Interpreten,
  Album-Interpreten, Komponisten, Jahre, Favoriten, Playlists,
  Warteschlange, Kürzlich hinzugefügt/gespielt, Häufig gespielt,
  Lang, Am besten/schlechtesten bewertet
- Suche über Titel/Interpret/Album
- Playlists anlegen, Titel hinzufügen, abspielen
- Favoriten (Herz) und Bewertungen (Daumen hoch/runter)
- Shuffle, Repeat (aus/alle/einzeln)
- Wiedergabe über ExoPlayer/Media3 mit Sperrbildschirm- und
  Benachrichtigungs-Steuerung (MediaSession)
- Cover-Art aus den Metadaten, alles läuft rein lokal/offline - kein
  Internetzugriff, keine Cloud

## Was (noch) nicht enthalten ist

- **Genres, Streams, Lesezeichen**: in der Bibliotheksübersicht als
  "bald verfügbar" markiert, aber noch nicht umgesetzt - Genres
  bräuchten eine zusätzliche MediaStore-Abfrage, Streams würde dem
  "alles offline"-Wunsch widersprechen, Lesezeichen (Sprungmarken in
  langen Dateien) ist ein eigenes kleines Feature für später.
- **Audio-Fingerprinting/Duplikate (dein "Music Cleaner")**: bewusst
  nicht in dieser ersten Version, wie besprochen - das ist ein
  eigenständiges, größeres Feature (Audio-Fingerprints berechnen und
  vergleichen), das wir als nächsten Schritt draufsetzen können,
  sobald der Player läuft.
- Gapless-Wiedergabe funktioniert so gut, wie es ExoPlayer von Haus
  aus kann (kein manuelles Crossfading o.ä.).

## Ehrlicher Hinweis zum ersten Build

Dieses Projekt wurde ohne Android Studio und ohne Internetzugriff
geschrieben - ich konnte es hier nicht kompilieren oder testen. Die
Chancen stehen gut, dass alles auf Anhieb baut, aber bei einem
Projekt dieser Größe (ExoPlayer/Media3-Audioverarbeitung,
Compose-UI, Room-Datenbank) ist es realistisch, dass der erste
GitHub-Actions-Lauf ein oder zwei Fehler zeigt - meistens kleine
Versions- oder API-Abweichungen.

Falls der Workflow rot wird: auf den fehlgeschlagenen Lauf klicken,
den Fehlertext (unter dem Schritt "Debug-APK bauen") kopieren und
mir schicken - genau wie beim UltraAI-Windows-Projekt gehen wir das
dann zusammen durch, bis es sauber durchläuft.

Am ehesten anfällig für kleine Anpassungen:
- `playback/PlaybackService.kt` (die Verdrahtung von ExoPlayer mit
  dem eigenen EQ-Audioprozessor - API-Namen können sich zwischen
  Media3-Versionen leicht unterscheiden)
- `ui/components/VerticalBandFader.kt` (die gedrehten Schieberegler -
  rein visuell, falls die Ausrichtung/Größe nicht optimal aussieht)

## Projektstruktur

```
app/src/main/java/com/rotherbaum/player/
  data/         Track-Modell, MediaStore-Scan, Room-Datenbank
  playback/     Biquad-DSP, EqualizerProcessor, PlaybackService, PlayerViewModel
  ui/           Compose-UI: Bibliothek, Equalizer, Suche, Player, Playlists
```
