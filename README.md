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
`http://localhost:8080`. The welcome screen checks the API and restores an
existing sign-in session when it loads.

## Accounts

The welcome screen supports account registration and sign-in. Accounts use a
display name and email address; passwords are stored as BCrypt hashes. The API
uses session cookies and CSRF tokens for browser requests:

- `GET /api/auth/csrf` — obtain a CSRF token before a state-changing request
- `POST /api/auth/register` — create an account and sign in
- `POST /api/auth/login` — sign in
- `GET /api/auth/me` — get the signed-in account
- `POST /api/auth/logout` — end the current session

For HTTPS deployments, set `SESSION_COOKIE_SECURE=true` so session and CSRF
cookies are only sent over secure connections. Flyway applies the SQL schema
migrations in `backend/src/main/resources/db/migration` at startup.

## Run locally

1. Install Node.js and Java 21 or newer.
2. Create a PostgreSQL database named `the_system` and a user with access to it.
   Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for that database.
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
