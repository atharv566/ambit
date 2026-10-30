# CPVTS — Tech Stack & Architecture Guide

## Overview

CPVTS (Campus Parking & Vehicle Tracking System) is a full-stack TypeScript monorepo. The frontend, backend, and database all live in one project and are started with a single command. It supports two database modes: a local embedded PostgreSQL (no setup needed) and a hosted Supabase PostgreSQL.

---

## Layer-by-Layer Breakdown

### 1. Frontend — React 19 + TypeScript

| What | Detail |
|------|--------|
| Framework | React 19 with functional components and hooks |
| Language | TypeScript (strict) |
| Bundler | Vite 7 |
| Icons | Lucide React |
| Styling | Plain CSS with CSS custom properties (no CSS framework) |
| State | `useState` / `useEffect` — no Redux or Zustand |

**How it works:**
- The entire UI lives in `src/main.tsx` as a single-file React app.
- Pages are: Dashboard, Check-in, Check-out, Vehicle Finder, Daily Summary.
- Navigation is hash-based (`#dashboard`, `#check-in`, etc.) — no React Router.
- The dashboard **polls every 15 seconds** via `setInterval` to keep occupancy fresh.
- All data comes from the Node API over same-origin HTTP — no direct database access from the browser.
- Formatting: Indian currency (₹), IST timezone (`Asia/Kolkata`), 24-hour clock.
- Vite serves the frontend in development; in production, the Express server serves the built `dist/` folder.

---

### 2. Backend — Node.js + Express 5 + TypeScript

| What | Detail |
|------|--------|
| Runtime | Node.js ≥ 22.12 |
| Framework | Express 5 |
| Language | TypeScript, executed directly by `tsx` (no compile step needed at runtime) |
| Validation | Zod 4 |
| DB client (Supabase) | `pg` (node-postgres) v8 |
| DB client (local) | `@electric-sql/pglite` |

**How it works:**
- Entry point: `server/index.ts` starts the HTTP server.
- Routes are defined in `server/app.ts` and follow a clean REST pattern under `/api/v1/`.
- Every request body is validated by a **Zod schema** before touching the database. Invalid input returns a `400 VALIDATION_ERROR` with field-level details.
- The server calls **PostgreSQL stored functions** (RPCs) — it never writes raw SQL for business logic. This keeps all fee rules and concurrency logic in the database.
- Cross-origin POST requests are blocked (no CORS). The app is designed to run on loopback (`127.0.0.1`) for campus-local use.
- Security headers are set on every `/api` response: `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`.
- Errors from the database are mapped to stable API error codes without leaking schema details.

**API endpoints:**

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/health` | Liveness check, returns mode |
| GET | `/api/v1/dashboard` | Full snapshot (slots + records + summary) |
| GET | `/api/v1/slots` | Slot list only |
| POST | `/api/v1/check-ins` | Park a vehicle |
| POST | `/api/v1/check-outs` | Release a vehicle, generate receipt |
| GET | `/api/v1/vehicles/:number` | Find an active vehicle |
| GET | `/api/v1/summary?date=YYYY-MM-DD` | Daily summary for a date |

---

### 3. Database — PostgreSQL (via PGlite or Supabase)

| What | Detail |
|------|--------|
| Engine | PostgreSQL (same schema for both modes) |
| Local mode | PGlite — embedded PostgreSQL in Node.js, persisted to `data/parking` |
| Hosted mode | Supabase — PostgreSQL over Session pooler, verified TLS |
| Schema file | `migrations/001_parking.sql` |

**Tables:**

- `cpvts_slots` — Fixed capacity table. 10 two-wheeler slots (T-01 to T-10) and 5 four-wheeler slots (F-01 to F-05). Immutable by the application.
- `cpvts_records` — Every parking session. Active sessions have a null `exit_hour`. Completed sessions have a receipt with fee details.

**Stored functions (RPCs):**

| Function | What it does |
|----------|-------------|
| `cpvts_check_in(...)` | Normalizes vehicle, checks duplicates, finds first free slot, inserts record — all in one transaction with an EXCLUSIVE lock |
| `cpvts_check_out(...)` | Locks record, validates exit hour, calculates fee, stores receipt, releases slot — atomic |
| `cpvts_find_vehicle(...)` | Returns the active session for a vehicle number |
| `cpvts_snapshot(...)` | Returns slots + active records + daily summary in one consistent read |

**Concurrency model:**
- Both write functions acquire an `EXCLUSIVE` lock on `cpvts_slots` before checking occupancy. This serializes all writes — no double-allocation possible even under concurrent requests.
- Partial unique indexes enforce: one active session per vehicle, one active session per slot.
- Composite foreign keys ensure a vehicle's type matches the slot's zone.

**Fee rules (inside `cpvts_check_out`):**

| Category | Two-wheeler | Four-wheeler |
|----------|-------------|--------------|
| Staff | Free | Free |
| Student | 2 hrs free, then ₹10/hr | 2 hrs free, then ₹20/hr |
| Visitor | ₹20/hr | ₹40/hr |

Duration = exit hour − entry hour. No overnight rollover.

---

### 4. Database Modes

#### Local mode (PGlite)
- Uses `@electric-sql/pglite` — a fully functional PostgreSQL engine compiled to WebAssembly, running inside the Node process.
- Data is persisted on disk at `./data/parking`.
- No credentials, no network, no setup. Works out of the box.
- Only one process can use the directory at a time.
- Set by `DATABASE_MODE=local` in `.env`.

#### Supabase mode
- Uses `pg` (node-postgres) with a connection pool of **4 connections**.
- Connects through the **Session pooler** (not the direct host — that's IPv6 only).
- TLS is always verified using the bundled Supabase CA at `./certs/supabase-ca.crt`. TLS is never disabled.
- Each query runs as `service_role` using `SET LOCAL ROLE` inside its transaction.
- Timeouts: 10s connection, 10s statement, 10s idle.
- Set by `DATABASE_MODE=supabase` + `DATABASE_URL` in `.env`.

#### Optional HTTP adapter (Supabase Data API)
- If `DATABASE_URL` is empty but `SUPABASE_URL` + `SUPABASE_SECRET_KEY` are set, the server uses Supabase's REST API instead of a direct connection.
- The `service_role` / `sb_secret_...` key is required — the anon key cannot access this project (RLS blocks it).

---

### 5. Build & Tooling

| Tool | Role |
|------|------|
| TypeScript 5.9 | Type checking across frontend and backend |
| Vite 7 | Frontend dev server + production bundler |
| tsx 4 | Runs TypeScript server files directly — no `tsc` compile needed at runtime |
| Node.js test runner | Built-in `--test` flag runs `tests/*.test.ts` |
| dotenv | Loads `.env` into `process.env` |
| Zod 4 | Schema validation for all API inputs |

**Scripts:**

```sh
npm run dev        # Start dev server (Vite frontend + tsx backend, hot reload)
npm run build      # Type-check + Vite production build
npm start          # Serve production build via tsx
npm test           # Run database/API tests
npm run db:migrate # Apply 001_parking.sql to configured database
npm run db:verify  # Run hosted checks (Supabase mode only)
```

---

## Data Flow Diagram

```
Browser (React)
    │
    │  HTTP JSON  /api/v1/*
    ▼
Express server (Node.js / tsx)
    │
    │  Zod validation
    │
    │  database.rpc("cpvts_*", params)
    ▼
Database adapter
    ├── LOCAL:    PGlite (embedded, ./data/parking)
    └── SUPABASE: pg pool → Session pooler → Supabase PostgreSQL
                                               (TLS verified, service_role)
```

---

## Security Notes

- **No user accounts or auth** — designed for trusted campus LAN use. Binds to `127.0.0.1` by default.
- **Credentials never reach the frontend** — `DATABASE_URL` and `SUPABASE_SECRET_KEY` are server-only. No `VITE_` prefix is used for secrets.
- **RLS enabled** on Supabase tables — anonymous and authenticated roles have no table or function access. Only `service_role` can operate parking.
- **Same-origin enforcement** — Cross-origin POST requests return `403`. No CORS headers are added.
- **Parameterized queries** — all database calls use parameters, never string interpolation.
- **No silent mutation retry** — if a network error occurs mid-mutation, the user is told to refresh before retrying. No automatic retry that could double-park a vehicle.

---

## Project File Map

```
cpvts/
├── src/
│   ├── main.tsx        ← Entire React UI (single file)
│   ├── api.ts          ← Typed fetch wrapper for /api/v1/*
│   ├── types.ts        ← Shared TypeScript types (Snapshot, ParkingRecord, etc.)
│   └── styles.css      ← CSS custom properties palette + all component styles
│
├── server/
│   ├── index.ts        ← HTTP server entry point (port, host, static serving)
│   ├── app.ts          ← Express routes + Zod validation + error handling
│   ├── database.ts     ← Database interface + mode selection logic
│   ├── postgres.ts     ← Supabase pg pool adapter
│   ├── migrate.ts      ← CLI: applies 001_parking.sql
│   └── verify-supabase.ts ← CLI: runs hosted verification checks
│
├── migrations/
│   └── 001_parking.sql ← Schema, seed slots, stored functions, grants
│
├── certs/
│   └── supabase-ca.crt ← Bundled public CA for TLS verification
│
├── docs/
│   ├── api-contract.md ← REST API spec for Android/CLI clients
│   ├── architecture.md ← Data flow, concurrency, trust boundary
│   └── techstack.md    ← This file
│
├── tests/
│   ├── parking.test.ts ← Database + API integration tests
│   └── postgres.test.ts
│
├── .env.example        ← Template (safe to commit)
├── .env                ← Your credentials (gitignored)
├── package.json
├── tsconfig.json
└── vite.config.ts
```

---

## Key Design Decisions

1. **Monorepo, single process** — Frontend and backend share one `npm` project. In dev, Vite proxies `/api/*` to Express. In production, Express serves the built frontend.

2. **No ORM** — The server calls named PostgreSQL functions. Business logic (fees, locking, validation) lives entirely in the database, not scattered across application code.

3. **No React Router** — Hash-based navigation keeps the bundle small and avoids SSR complexity for a single-page campus tool.

4. **tsx over ts-node** — `tsx` starts faster and handles ESM TypeScript without a separate compilation step, making the dev loop instant.

5. **PGlite for local dev** — Developers get a fully functional PostgreSQL environment with zero external dependencies. The same SQL migration runs on both PGlite and Supabase.
