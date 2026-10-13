# Session authentication

The backend uses Spring Security 7.1.1, managed by Spring Boot 4.1.1. Registration
still creates an account without logging it in. Login uses `DaoAuthenticationProvider`
with the existing BCrypt `PasswordEncoder` and the repository-backed `UserDetailsService`.

## Endpoint contracts

| Method | Path | Access | Successful response |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | Public | 200: `{ "headerName": "X-XSRF-TOKEN", "token": "..." }` |
| POST | `/api/auth/register` | Public, CSRF required | 201: `{ "id", "fullName", "email" }` |
| POST | `/api/auth/login` | Public, CSRF required | 200: `{ "id", "fullName", "email" }` |
| GET | `/api/auth/me` | Authenticated | 200: `{ "id", "fullName", "email" }` |
| POST | `/api/auth/logout` | Authenticated, CSRF required | 204, empty body |

Invalid input returns 400 with field errors. Duplicate registration returns 409.
Wrong passwords and unknown accounts both return 401 with `Invalid email or password`.
Missing authentication returns 401 with `Authentication is required`. Missing or
invalid CSRF returns 403. Unrelated server/database failures retain safe 500 responses.
Errors use the existing `ApiErrorResponse` shape; they contain no exception or SQL details.

Registration keeps its minimum of eight characters. Both registration and login
reject passwords exceeding 72 UTF-8 bytes with a 400 field-validation response,
before BCrypt runs. This is a byte limit, so multibyte characters reach it sooner
than ASCII characters. Passwords are never trimmed, normalized, or truncated.

Other `/api/**` routes require authentication. Non-API routes are denied. Actual
servlet error dispatches are permitted so framework errors can finish rendering.
Unsupported methods on the public register/login URLs reach MVC's 405 handling.

## Session lifecycle

Successful login invokes Spring's session ID change strategy and CSRF authentication
strategy before creating a fresh security context. The controller explicitly saves
that context through the same repository used by the security filter chain.
Authentication credentials and the principal's temporary hash are erased before
the context is saved. The session contains account identity, not a persisted User entity.

`/me` reads only the authenticated principal; a supplied client ID cannot select
another account. Its identity fields are a snapshot from login. Logout is a protected
controller operation delegating to Spring's CSRF, security-context, session-invalidation,
and cookie-clearing handlers. It expires `JSESSIONID` and clears `XSRF-TOKEN`.

Sessions are server-side, in this application instance's memory, with a 30-minute
idle timeout and cookie-only tracking. They expire on application restart; shared
session storage and multi-instance deployment are deferred.

## Browser CSRF and CORS flow

1. Fetch `GET /api/auth/csrf` with `credentials: 'include'`.
2. The response sets readable `XSRF-TOKEN`; retain the returned token in memory.
3. Send that token as `X-XSRF-TOKEN` on registration, login, and logout POSTs, also
   using `credentials: 'include'`. The browser supplies the matching cookie.
4. After login, fetch `/api/auth/csrf` again before another POST. Login clears the
   previous token cookie. Fetch again after logout before another registration/login.

Spring Security's `csrf.spa()` resolves raw token headers while maintaining its
masked request-attribute handling. The bootstrap deliberately reads the raw
`DeferredCsrfToken`, so its JSON token matches the cookie and SPA header. Security
response headers prevent caching. The JSON bootstrap also works when same-site
frontend/API hosts differ and frontend JavaScript cannot read the API host's cookie.

`JSESSIONID` is HttpOnly; JavaScript must never read it. CSRF tokens are not login
credentials. Do not put either token or session ID in URLs, logs, localStorage,
or sessionStorage. Cookie CSRF uses a double-submit comparison: rotation changes
the browser cookie, but does not server-revoke an old matching cookie/header pair
deliberately restored by a client.

Development allows exactly `http://localhost:3000`, credentials, GET/POST/OPTIONS,
and `Content-Type`/`X-XSRF-TOKEN` headers. It never reflects arbitrary origins or
uses a wildcard origin with credentials. Configure one exact origin with
`FRONTEND_ORIGIN` (scheme, host, optional port; no path or wildcard).

Use `localhost` consistently for frontend and API; mixing it with `127.0.0.1`
changes origins/sites. These two localhost ports are different origins but the
same site. Cookies are host-scoped, not port-scoped.

## Local HTTP and production HTTPS

Local defaults: `JSESSIONID` has HttpOnly, SameSite=Lax, Path=/, and Secure=false.
The CSRF cookie has SameSite=Lax, Path=/, HttpOnly=false, and Secure=false.
Neither cookie has a broad domain. `APP_COOKIE_SECURE=true` can enable secure
cookies outside the production profile, for HTTPS environments.

For production HTTPS, set `SPRING_PROFILES_ACTIVE=prod` and explicitly set
`FRONTEND_ORIGIN`, for example `https://app.example.com`. The production profile
forces Secure=true on both cookies and does not fall back to the development origin.
HTTPS must actually serve the browser-facing application; this task does not set
up TLS or a reverse proxy. Secure cookies cannot support local HTTP testing.

SameSite=Lax supports same-site frontend/API deployments, such as
`https://app.example.com` and `https://api.example.com`. A truly cross-site deployment
needs a separately considered cookie/CSRF arrangement; changing CORS alone will
not make Lax cookies cross-site. Readable CSRF cookies depend on keeping XSS out
of the frontend. Rate limiting and account-wide session revocation are deferred.

Framework request, validation, security, and sensitive JDBC diagnostics are kept
out of logs. Do not override those categories to DEBUG/TRACE around real credentials.

## Manual verification in PowerShell

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` using your existing local
environment setup. Do not put their values in source files or print them.
From the repository root:

```powershell
Set-Location "D:\Styling app\apps\api"
.\mvnw.cmd spring-boot:run
```

In a second terminal, keep one `WebRequestSession` throughout. Only status codes
and safe account response bodies are displayed below, not cookie headers or token values.
The sample password is for a development test account only.

```powershell
$api = "http://localhost:8080"
$browser = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Get-CsrfHeaders {
    $csrf = Invoke-RestMethod -Uri "$api/api/auth/csrf" -WebSession $browser
    @{ $csrf.headerName = $csrf.token }
}

$headers = Get-CsrfHeaders
$email = "session-test-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"
$registerBody = @{
    fullName = "Session Test"
    email = $email
    password = "ExamplePassword123"
} | ConvertTo-Json

$registration = Invoke-WebRequest -UseBasicParsing `
    -Uri "$api/api/auth/register" -Method Post `
    -ContentType "application/json" -Body $registerBody `
    -Headers $headers -WebSession $browser
$registration.StatusCode                       # 201
$registration.Content | ConvertFrom-Json        # safe account fields

# Registration alone does not establish authentication: expect 401.
try {
    Invoke-RestMethod -Uri "$api/api/auth/me" -WebSession $browser
} catch {
    [int]$_.Exception.Response.StatusCode
}

$loginBody = @{ email = $email; password = "ExamplePassword123" } | ConvertTo-Json
$login = Invoke-WebRequest -UseBasicParsing `
    -Uri "$api/api/auth/login" -Method Post `
    -ContentType "application/json" -Body $loginBody `
    -Headers $headers -WebSession $browser
$login.StatusCode                              # 200
$login.Content | ConvertFrom-Json               # safe account fields

# Login rotated CSRF. Refresh it before the next POST.
$headers = Get-CsrfHeaders
Invoke-RestMethod -Uri "$api/api/auth/me" -WebSession $browser  # authenticated account

$logout = Invoke-WebRequest -UseBasicParsing `
    -Uri "$api/api/auth/logout" -Method Post `
    -Headers $headers -WebSession $browser
$logout.StatusCode                             # 204

try {
    Invoke-RestMethod -Uri "$api/api/auth/me" -WebSession $browser
} catch {
    [int]$_.Exception.Response.StatusCode        # 401 after logout
}

# Ready for another login or registration attempt.
$headers = Get-CsrfHeaders
```

Additional checks: repeat registration with that email and valid CSRF (409), submit
a wrong password with valid CSRF (401), or omit `-Headers` on a POST (403). Do not
display the whole `WebRequestSession`, request bodies, or response headers.

Safe Neon verification:

```sql
SELECT id, full_name, email, created_at, updated_at
FROM app_users
ORDER BY id DESC;
```

## Automated verification

Run from `apps/api`:

```powershell
.\mvnw.cmd '-Dtest=RegisterRequestTests,AuthServiceTests,AuthControllerTests,LoginRequestTests,RepositoryUserDetailsServiceTests,PasswordAuthenticationTests,SessionAuthenticationTests,SessionCookieTests' test
```

Unit/MVC tests mock persistence and need no database. The embedded HTTP test uses
a separate test application without datasource auto-configuration and mocks the
repository/service, exercising real servlet session cookies. The existing full
application-context test remains unchanged and needs the real DB environment.

No JWT, OAuth, password reset, frontend integration, roles, or deployment
infrastructure is implemented.
