# Local Commands

Run these commands from the workspace root in Windows PowerShell. Keep the backend and frontend running in separate terminals.

## Prerequisites

Use JDK 21, not a JRE. The path below matches this machine; adjust it if your JDK is installed elsewhere.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java --version
javac --version
```

## Backend

In terminal 1:

```powershell
Set-Location backend\stockpulse
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Backend URL: `http://localhost:8080`

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

H2 console: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:stockpulse`
- User: `sa`
- Password: blank

The database is local and in-memory. It resets when the backend stops; no PostgreSQL connection or seed data is used.

## Frontend

In terminal 2, from the workspace root:

```powershell
Set-Location frontend
npm install
npm run lint
npm run build
npm run dev
```

Open `http://localhost:5173`. Vite uses `http://localhost:8080/api` by default. Leave the development servers running in their terminals; press `Ctrl+C` in each terminal to stop them.

## Optional LLM

To enable LLM requests, copy the backend example into a local `.env` file from the backend directory, then edit that local file:

```powershell
Set-Location backend\stockpulse
Copy-Item .env.example .env
notepad .env
```

Spring Boot loads `backend/stockpulse/.env` as properties when the backend starts. Put your rotated key in `LLM_API_KEY`; the example leaves it blank. The `.env` file is Git-ignored. If the key is unset, the app uses its rule-based advisor. Never paste a working key into source files or commit it. The supplied `llm.txt` contains credential-like values; rotate them and do not send its cookie header.
