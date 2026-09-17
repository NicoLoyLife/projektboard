# Projektboard

Webanwendung zur Verwaltung von Projekten, Aufgaben und Mitarbeitenden für ein mittelständisches IT-Dienstleistungsunternehmen. Entstanden als Fallstudie im Kurs Programmierung von Web-Anwendungen.

Das Projekt befindet sich im Aufbau. Diese README wächst mit den Funktionen.

## Technologien

- Backend: Spring Boot 4.1 mit Spring MVC, Spring Data JPA, Spring Security und Bean Validation, Java 21, Maven
- Datenbank: H2 im Dateimodus
- Frontend: React 19 mit Vite, TypeScript und Material UI

## Voraussetzungen

- Java 21
- Node.js 24 mit npm

## Backend starten

```
cd backend
./mvnw spring-boot:run
```

Die API ist danach unter http://localhost:8080/api erreichbar. Die Datenbankdatei entsteht beim ersten Start unter `backend/data/`.

## Frontend starten

```
cd frontend
npm install
npm run dev
```

Die Oberfläche läuft unter http://localhost:5173 und leitet Aufrufe von `/api` an das Backend weiter.

## Tests ausführen

```
cd backend
./mvnw test
```

## Lizenz

MIT, siehe [LICENSE](LICENSE).
