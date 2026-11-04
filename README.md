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
- `POST /api/auth/email/otp/request` — request a one-time email sign-in code
- `POST /api/auth/email/otp/verify` — verify the code and sign in
- `GET /api/auth/me` — get the signed-in account
- `POST /api/auth/logout` — end the current session

Email-code sign-in is available to verified accounts. Codes expire after 10
minutes, can be used once, and allow up to five verification attempts. Requests
are rate limited to one code per minute and return the same response whether or
not the email belongs to an eligible account. OTP request processing and email
delivery are asynchronous so the request response does not wait on account
lookups or the mail server. Failed code guesses also count toward an account
limit of 10 attempts per hour; reaching that limit blocks email-code sign-in for
15 minutes. A successful sign-in clears the account failure counter.

For HTTPS deployments, set `SESSION_COOKIE_SECURE=true` so session and CSRF
cookies are only sent over secure connections. Flyway applies the SQL schema
migrations in `backend/src/main/resources/db/migration` at startup.

## Local environment configuration

The repository uses a root `.env` file for local development configuration.
Copy the example file and fill in your own values:

```powershell
Copy-Item .env.example .env
```

The root `.env` file is loaded by the backend at startup and by Vite for
frontend development. The backend loader also checks the backend working
directory, so a `.env` file there can be used if needed. Real `.env` files
are ignored by Git; only `.env.example` is committed.

Never put secrets in variables prefixed with `VITE_`: Vite exposes those values
to browser code. Keep database passwords, OAuth client secrets, and mail
credentials in server-side variables only.

## Google sign-in

Google sign-in uses Spring Security OAuth2 Client. Set these values in the root
`.env` file:

- `APP_SECURITY_GOOGLE_ENABLED=true`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=openid,profile,email`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI=http://localhost:5173/login/oauth2/code/google`
- `APP_FRONTEND_URL=http://localhost:5173`

In Google Cloud Console, configure the OAuth client as a **Web application** and
add this authorized JavaScript origin:

`http://localhost:5173`

Add this authorized redirect URI:

`http://localhost:5173/login/oauth2/code/google`

The Vite development server proxies `/api`, `/oauth2`, and
`/login/oauth2` to the backend. For a deployed environment, configure the
production frontend URL and exact HTTPS redirect URI registered with Google.
Do not expose the Google client secret to the frontend or commit it to Git.

## Run locally

1. Install Node.js and Java 21 or newer.
2. Create a PostgreSQL database named `the_system` and configure its connection
   values in the root `.env` file.
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
