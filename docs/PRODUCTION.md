# Production deployment

No Docker — the backend runs as a plain `java -jar` process, the frontend is a static build
served by any web server, and a reverse proxy in front of the backend handles TLS.

## Backend

Build:

```bash
cd backend
./mvnw clean package
```

This produces `target/exhibitor-registration-0.0.1-SNAPSHOT.jar`, a self-contained runnable jar
(Flyway migrates the schema automatically on boot — no separate migration step).

Run with `SPRING_PROFILES_ACTIVE=prod` and the following environment variables. Every one of
these is **required** — `application-prod.yml` has no fallback defaults for them, so the app
fails fast on boot rather than silently using a dev-only value:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | e.g. `jdbc:postgresql://<db-host>:5432/<db-name>` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Postgres credentials |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` | Redis connection |
| `APP_JWT_SECRET` | A strong random secret — **never reuse the one from local dev's `application-local.yml`** |
| `APP_CORS_ALLOWED_ORIGINS` | The real frontend origin(s), e.g. `https://app.yourdomain.example` (comma-separated for more than one) |
| `APP_PUBLIC_FRONTEND_BASE_URL` | e.g. `https://app.yourdomain.example/register/` — used to build the links Organisers generate |
| `RECAPTCHA_SECRET_KEY` | A real Google reCAPTCHA v2 secret key (matching the frontend's real site key — see below) |

Optional:

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | Plain-HTTP listen port behind your reverse proxy |
| `SERVER_SSL_ENABLED` | `false` | Set `true` only if this process should terminate TLS itself (needs `SSL_KEYSTORE_PATH`/`SSL_KEYSTORE_PASSWORD` too) — not the recommended setup |
| `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` | unset | Only takes effect once, on a genuinely empty `users` table. Treat the password as one-time — the account is forced to change it on first login. |

Run it:

```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://... \
... (the rest of the required vars above) \
java -jar target/exhibitor-registration-0.0.1-SNAPSHOT.jar
```

In practice, run this under a process manager (systemd, pm2, supervisor) so it restarts on
crash/reboot rather than invoking it directly in a terminal.

## Frontend

```bash
cd frontend
cp .env.production.example .env.production.local   # then fill in the real values
npm run build
```

This produces `dist/` — plain static files, deployable to any web server, static host, or CDN.

**Client-side routing needs SPA fallback.** React Router handles routes like `/admin/actors`
entirely in the browser; a hard refresh or direct link to that URL needs the web server to
serve `index.html` for any path that isn't a real file, or it 404s. Example nginx block:

```nginx
server {
    listen 443 ssl;
    server_name app.yourdomain.example;
    root /var/www/exhibitor-frontend/dist;

    location / {
        try_files $uri /index.html;
    }
}
```

## Reverse proxy for the backend

The backend listens on plain HTTP (`SERVER_PORT`, default 8080). Terminate TLS in front of it
with a real certificate (e.g. via certbot/Let's Encrypt). Example nginx block:

```nginx
server {
    listen 443 ssl;
    server_name api.yourdomain.example;
    # ssl_certificate / ssl_certificate_key from certbot or your CA of choice

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Security checklist

- [ ] `APP_JWT_SECRET` is a strong random value, generated fresh for this deployment — not the
      one from local dev.
- [ ] `VITE_RECAPTCHA_SITE_KEY` (frontend) and `RECAPTCHA_SECRET_KEY` (backend) are a real,
      matching Google reCAPTCHA v2 key pair for your domain — not the committed test key.
- [ ] `ADMIN_BOOTSTRAP_PASSWORD` was a one-time value; the resulting account has already changed
      its password (enforced automatically on first login).
- [ ] Postgres and Redis are not publicly reachable — bind to localhost or a private network,
      reachable only from the backend.
- [ ] `/swagger-ui/**` and `/v3/api-docs/**` are reachable by default in every profile
      (`SecurityConfig`'s public paths, unchanged from dev). If you'd rather keep API docs
      private, block those paths at the reverse-proxy layer — no backend code change needed.
