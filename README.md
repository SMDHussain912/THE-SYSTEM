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
   Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as environment
   variables.
3. Start the backend:

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

## Google sign-in

Google sign-in uses Spring Security OAuth2 Client. Configure the following
environment variables on the machine that runs the backend:

| Environment variable | Value |
| --- | --- |
| `APP_SECURITY_GOOGLE_ENABLED` | `true` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` | Your Google OAuth client ID |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | Your Google OAuth client secret |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE` | `openid,profile,email` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI` | `http://localhost:5173/login/oauth2/code/google` |
| `APP_FRONTEND_URL` | `http://localhost:5173` |

In Google Cloud Console, configure the OAuth client as a **Web application** and
add this authorized JavaScript origin:

`http://localhost:5173`

Add this authorized redirect URI:

`http://localhost:5173/login/oauth2/code/google`

On Windows, add these values under **Start → search “Edit the system
environment variables” → Environment Variables → User variables**. Close and
reopen PowerShell, Command Prompt, your IDE, or any terminal that runs the
backend so it inherits the new environment. You do not need to put credentials
in a command or commit them to the repository.

The Vite development server proxies `/api`, `/oauth2`, and
`/login/oauth2` to the backend. Keep the Google client secret private. For a
deployed environment, set the same variables to the production client ID,
secret, frontend URL, and exact HTTPS redirect URI registered with Google.

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
