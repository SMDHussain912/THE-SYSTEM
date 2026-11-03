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


## Google sign-in (local development)

Google sign-in is implemented with Spring Security OAuth2 Client. It is disabled
by default, so the application can still start without Google credentials.

1. In the [Google Cloud Console](https://console.cloud.google.com/), create or
   select a project and configure the OAuth consent screen.
2. Create an OAuth client ID with application type **Web application**.
3. Add this authorized JavaScript origin:

   `http://localhost:5173`

4. Add this authorized redirect URI:

   `http://localhost:5173/login/oauth2/code/google`

5. Keep the generated client secret private. In PowerShell, set the credentials
   in the same terminal where you start the backend:

   ```powershell
   $env:GOOGLE_CLIENT_ID = "your-client-id"
   $env:GOOGLE_CLIENT_SECRET = "your-client-secret"
   ```

6. Start the backend with the Google OAuth profile:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=google-oauth"
   ```

7. Start the frontend separately with `npm run dev` and open
   `http://localhost:5173`.

The Vite development server proxies `/api`, `/oauth2`, and
`/login/oauth2` to the backend. For a deployed environment, register the exact
HTTPS redirect URI used by that deployment and set `GOOGLE_REDIRECT_URI`,
`FRONTEND_URL`, `GOOGLE_CLIENT_ID`, and `GOOGLE_CLIENT_SECRET` accordingly.
Never commit OAuth client secrets to the repository.

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
