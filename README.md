# Projektboard

Webanwendung zur Verwaltung von Projekten, Aufgaben und Mitarbeitenden für ein mittelständisches IT-Dienstleistungsunternehmen. Das Projekt ist als Fallstudie im Kurs Programmierung von Web-Anwendungen entstanden.

Bisher wurden Aufgaben, Projektfortschritte und Zuständigkeiten in Tabellenblättern gepflegt. Diese Anwendung löst das ab und stellt Zugriffsrechte, einen gemeinsamen Stand und einen jederzeit aktuellen Fortschritt bereit.

## Funktionen

Rollen: Administration, Projektleitung und Mitarbeitende.

- Anmelden und Abmelden, ohne Anmeldung ist keine Seite erreichbar.
- Die Administration legt Benutzerkonten an, vergibt Rollen und deaktiviert Konten.
- Die Projektleitung legt Projekte an, bearbeitet und archiviert sie.
- Die Projektleitung ordnet einem Projekt Mitarbeitende zu.
- Leitung und Mitglieder legen Aufgaben in einem Projekt an und bearbeiten sie.
- Der Status einer Aufgabe wechselt zwischen offen, in Bearbeitung und erledigt.
- Der Fortschritt eines Projekts ergibt sich aus dem Anteil erledigter Aufgaben.
- Sichtbar sind nur Projekte, die man leitet oder in denen man Mitglied ist. Die Administration liest alle Projekte, greift fachlich aber nicht ein.

Archivierte Projekte bleiben lesbar, lassen sich aber nicht mehr ändern. Benutzer werden deaktiviert statt gelöscht, damit ihre Zuordnungen erhalten bleiben. Die Trennung mehrerer Mandanten ist im Datenmodell vorbereitet, die Anwendung arbeitet mit einem Standardmandanten.

## Technologien

| Bereich | Einsatz |
|---|---|
| Backend | Spring Boot 4.1 mit Spring MVC, Spring Data JPA, Spring Security und Bean Validation |
| Sprache und Build | Java 21, Maven mit Wrapper |
| Datenbank | H2 im Dateimodus, Zugriff über JPA und Hibernate |
| Frontend | React 19 mit Vite, TypeScript und Material UI |
| Tests | JUnit, Mockito, AssertJ und MockMvc |

## Voraussetzungen

- Java 21
- Node.js 24 mit npm

## Backend starten

```
cd backend
./mvnw spring-boot:run
```

Die API ist danach unter http://localhost:8080/api erreichbar. Beim ersten Start entsteht die Datenbankdatei unter `backend/data/` und wird mit Demo-Daten gefüllt. Ein Löschen dieses Verzeichnisses setzt den Stand zurück.

## Frontend starten

```
cd frontend
npm install
npm run dev
```

Die Oberfläche läuft unter http://localhost:5173 und leitet Aufrufe von `/api` an das Backend weiter. Beide Teile müssen gleichzeitig laufen.

## Demo-Konten

Die Konten dienen nur der Demonstration. Das Passwort lautet bei allen `demo1234`.

| Benutzername | Rolle | Sieht |
|---|---|---|
| admin | Administration | alle Projekte lesend, Benutzerverwaltung |
| leitung | Projektleitung | drei eigene Projekte, davon eines archiviert |
| anna | Mitarbeitende | zwei Projekte |
| ben | Mitarbeitende | zwei Projekte, davon eines archiviert |
| chris | Mitarbeitende | kein Projekt |

## Tests ausführen

```
cd backend
./mvnw test
```

Die Tests laufen gegen eine Datenbank im Arbeitsspeicher und lassen die Datei unter `backend/data/` unberührt. Geprüft werden die Geschäftsregeln der Services, die Abfragen der Repositories, die Rollen- und Validierungsregeln der Schnittstelle sowie der Anmeldeablauf und die Sichtbarkeit über die gesamte Anwendung.

Im Frontend prüfen `npm run build` die Typen und `npm run lint` den Code.

## Projektstruktur

```
backend/          Spring-Boot-Anwendung
  src/main/java/io/github/nicoloylife/projektboard/
    domain/       Entitäten und Aufzählungen
    repository/   Datenzugriff über Spring Data JPA
    service/      Geschäftsregeln und fachliche Ausnahmen
    api/          REST-Controller, DTOs und zentrale Fehlerbehandlung
    security/     Anmeldung, Rollen und Zugriffsschutz
    bootstrap/    Demo-Daten beim ersten Start
frontend/         React-Anwendung
  src/api/        Aufrufe der Schnittstelle und Typen
  src/auth/       Sitzungszustand und Schutz der Seiten
  src/components/ Navigationsleiste, Dialoge und wiederverwendbare Bausteine
  src/pages/      Anmeldung, Projektübersicht, Projektdetail, Benutzerverwaltung
```

## Schnittstelle

| Methode | Pfad | Zweck |
|---|---|---|
| POST | /api/auth/login | anmelden |
| POST | /api/auth/logout | abmelden |
| GET | /api/auth/me | eigene Identität und Rolle |
| GET | /api/auth/csrf | Token für schreibende Aufrufe |
| GET, POST | /api/users | Benutzer auflisten und anlegen |
| PUT | /api/users/{id} | Benutzer ändern |
| GET, POST | /api/projects | sichtbare Projekte auflisten und anlegen |
| GET, PUT | /api/projects/{id} | Projekt lesen und ändern |
| PATCH | /api/projects/{id}/status | archivieren oder reaktivieren |
| PUT | /api/projects/{id}/members | Mitglieder setzen |
| GET | /api/projects/{id}/member-candidates | zuordenbare Benutzer |
| POST | /api/projects/{id}/tasks | Aufgabe anlegen |
| PUT | /api/tasks/{id} | Aufgabe ändern |
| PATCH | /api/tasks/{id}/status | Status wechseln |

Fehler antworten einheitlich mit einer Kennung und einer Meldung, Validierungsfehler zusätzlich mit den betroffenen Feldern.

## Hinweise zum Betrieb

Die Anwendung ist für die Entwicklung und die Demonstration eingerichtet. Für einen produktiven Einsatz wären mindestens eine dauerhaft betriebene Datenbank, versionierte Schemaänderungen und die Auslieferung über HTTPS nötig.

## Lizenz

MIT, siehe [LICENSE](LICENSE).
