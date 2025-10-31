# JobScopeAI Backend

Small Spring Boot backend and Vite+React frontend that aggregates job listings from multiple providers (Adzuna, RapidAPI hosts) and exposes a simple `/api/jobs` endpoint used by the frontend.

## Features
- Aggregates job data from provider adapters (Adzuna, RapidAPI)
- Normalizes different provider responses into a simple job schema
- In-memory caching and basic rate-limit safeguards

## Requirements
- Java 21 (JDK 21)
- Maven
- Node.js + npm (for frontend)

## Environment variables
Set the following variables before running the backend locally. Use PowerShell `setx` for persistence or `set`/`$env:` for a single session.

- ADZUNA_APP_ID — Adzuna application id
- ADZUNA_APP_KEY — Adzuna application key
- ADZUNA_COUNTRY — country code (e.g., `us` or `gb`)
- RAPIDAPI_KEY — RapidAPI X-RapidAPI-Key
- RAPIDAPI_PROVIDERS — comma-separated provider entries like `host|/path` (see examples in code and docs)
- HUGGINGFACE_API_TOKEN — optional, for model-based summarization
- CACHE_TTL_SECONDS — optional cache TTL seconds (defaults in code)
- PROVIDER_MIN_INTERVAL_MS — optional per-provider minimum interval

Example (PowerShell persistent):

```powershell
setx ADZUNA_APP_ID "<your_id>"
setx ADZUNA_APP_KEY "<your_key>"
setx ADZUNA_COUNTRY "us"
setx RAPIDAPI_KEY "<your_rapidapi_key>"
setx RAPIDAPI_PROVIDERS "adzuna.p.rapidapi.com|/v1/api/jobs/us/search/1"
```

Open a new shell after `setx` for values to take effect.

## Run backend

Make sure your shell uses Java 21 (example)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

If you see an UnsupportedClassVersionError, your `mvn` or `java` is running an older JDK. Verify with:

```powershell
java -version
mvn -v
```

Or package and run the jar explicitly with the JDK21 `java`:

```powershell
mvn -DskipTests package
& 'C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot\bin\java.exe' -jar target\jobscopeai-backend-0.0.1-SNAPSHOT.jar
```

## Run frontend (dev)

```powershell
cd frontend
npm install
npm run dev
# open http://localhost:5173
```

## Testing providers
- To test Adzuna only, set ADZUNA_* variables and clear `RAPIDAPI_PROVIDERS`.
- Call the aggregator:

```powershell
curl "http://localhost:8080/api/jobs?providers=adzuna"
```

Errors like 401/403 typically mean invalid API keys.

## Troubleshooting
- If `mvn spring-boot:run` exits with code 1 and logs show `UnsupportedClassVersionError`, ensure Maven/Java are on JDK21.
- If the backend cannot reach a provider, check your API keys and network/firewall.
- The project currently includes Spring Data MongoDB on the classpath; if you don't run MongoDB you may see connection attempts in logs — these are informational unless your app explicitly requires Mongo.

## Contributing
- Add adapters in `src/main/java/com/jobscopeai/provider` and update `JobController` to call them.

---
Small demo project created to show provider aggregation, normalization and a minimal frontend.
# JobScopeAI Backend

Lightweight Spring Boot backend for aggregating job listings from multiple providers (Adzuna, RapidAPI hosts). Includes a small React + Vite frontend in `/frontend`.

## What it is
- Java 21 Spring Boot backend that proxies and normalizes job search results from public job provider APIs.
- Adapters for Adzuna and RapidAPI-style providers.
- In-memory caching and simple provider rate-limit safeguards.
- Frontend scaffold in `frontend/` (React + Vite + TypeScript) that queries `/api/jobs`.

## Quickstart (local)

Prerequisites
- Java 21 (JDK)
- Maven
- Node.js + npm (for frontend)

1. Set environment variables (PowerShell examples)

```powershell
# Adzuna (recommended for initial testing)
setx ADZUNA_APP_ID "<your_adzuna_app_id>"
setx ADZUNA_APP_KEY "<your_adzuna_app_key>"
setx ADZUNA_COUNTRY "us"

# RapidAPI (optional)
setx RAPIDAPI_KEY "<your_rapidapi_key>"
# Format: host|/path,comma-separated
setx RAPIDAPI_PROVIDERS "linkedin-job-search-api.p.rapidapi.com|/active-jb-1h?offset=0&description_type=text"

# (Optional) Hugging Face token for agent/summarization features
setx HUGGINGFACE_API_TOKEN "hf_xxx"
```

Open a new terminal after `setx` to load the variables.

2. Start the backend

```powershell
mvn spring-boot:run
```

If you see a class version error, make sure Maven and Java use JDK 21. See Troubleshooting below.

3. Start the frontend (optional)

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL (usually http://localhost:5173) and try searches.

## API
- GET /api/jobs?skills={skills}&location={location}&providers={provider1,provider2}

## Configuration
- `ADZUNA_APP_ID`, `ADZUNA_APP_KEY`, `ADZUNA_COUNTRY`
- `RAPIDAPI_KEY`, `RAPIDAPI_PROVIDERS` (comma-separated host|path entries)
- `CACHE_TTL_SECONDS` — TTL for in-memory cache (optional)
- `PROVIDER_MIN_INTERVAL_MS` — minimal interval between calls to a provider (optional)

## Troubleshooting
- Class version errors: ensure `java -version` and `mvn -v` show Java 21. You can set the current shell to JDK21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -v
```

- If Spring Boot tries to connect to MongoDB and you don't have Mongo installed you'll see connection errors; remove `spring-data-mongodb` if you don't need it, or run a local Mongo instance.

## Development notes
- Adapters live under `src/main/java/com/jobscopeai/provider/`
- `Normalizer.java` maps provider fields to a common job schema.
- The backend returns a normalized JSON array consumed by the frontend.

## License
This repository is a demo; ensure you follow API provider terms of service when using provider APIs.

---
If you want, I can also push this README to the `main` branch on GitHub (I will run git add/commit/push). Tell me to proceed.
