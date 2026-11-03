# The System

A web-first personal progress application. The repository starts with separate
React and Spring Boot apps; product features and game mechanics are intentionally
left open for planning.

## Stack

- Frontend: React and Vite (JavaScript)
- Backend: Spring Boot (Java 21)
- Database: PostgreSQL

## Project layout

- `frontend/` — React web app
- `backend/` — REST API and PostgreSQL integration

The frontend's development server proxies `/api` requests to the backend at
`http://localhost:8080`. The starter page checks `GET /api/health` to show whether
the API is reachable.

## Run locally

1. Install Node.js and Java 21 or newer.
2. Create a PostgreSQL database named `the_system` and a user with access to it.
   The backend defaults to `the_system` / `the_system_dev` on localhost. Override
   these local-development defaults with `DB_URL`, `DB_USERNAME`, and
   `DB_PASSWORD` environment variables as needed.
3. In a terminal, start the backend:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

4. In another terminal, start the frontend:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

Open the Vite URL printed in the frontend terminal. The API health check is
available at `http://localhost:8080/api/health`.

## Verify

```powershell
cd frontend
npm run lint
npm run build
```

```powershell
cd backend
.\mvnw.cmd test
```

Backend tests use an in-memory H2 database; the application itself uses
PostgreSQL.
