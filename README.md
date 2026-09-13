# StudyForge

**An intelligent academic target & project planning assistant for university students.**

> Say the goal → AI understands it → AI asks only what's missing → AI proposes a plan → you review, edit, and execute.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment variables](#environment-variables)
3. [Run with Docker (recommended)](#run-with-docker-recommended)
4. [Database options: local Mongo or MongoDB Atlas](#database-options-local-mongo-or-mongodb-atlas)
5. [Run locally without Docker](#run-locally-without-docker)
6. [Run tests](#run-tests)
7. [Environment variable reference](#environment-variable-reference)
8. [Project layout](#project-layout)
9. [Stack](#stack)
10. [Features](#features)
11. [AI principle](#ai-principle)

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| **Docker Desktop** | 24+ | Required for the Docker path |
| **Docker Compose** | v2 (`docker compose`) | Bundled with Docker Desktop |
| **Java** | 21 | Required only for the local (no-Docker) path |
| **Maven** | 3.9+ | Required only for the local path |
| **MongoDB** | 7 | Required only for the local path (Docker spins one up automatically; or point at Atlas) |
| **Gemini API key** | — | Free tier at [aistudio.google.com/apikey](https://aistudio.google.com/apikey). AI features show a friendly error without one; manual mode works fine. |

---

## Environment variables

Copy `.env.example` to `.env` and fill in at minimum:

```bash
cp .env.example .env
```

```env
GEMINI_API_KEY=AIza...   # your real Google AI Studio key
```

> `.env` contains secrets and is **git-ignored — never commit it.**

---

## Run with Docker (recommended)

This is the fastest path — no Java or MongoDB installation needed.

```bash
# 1. Copy and fill the env file
cp .env.example .env
#    edit .env — set GEMINI_API_KEY=AIza...

# 2. Build and start
docker compose up --build
```

Docker Compose will:
- Start a **MongoDB 7** container at `localhost:27017` as a local fallback database
- Build the Spring Boot app from `backend/Dockerfile` (multi-stage, Java 21)
- Serve everything at **http://localhost:8081**

> The host port is `8081` (mapped from the container's `8080`).
> Override it by setting `APP_PORT=<port>` in your `.env`.

**Stop the stack:**

```bash
docker compose down
```

**Stop and wipe the database volume:**

```bash
docker compose down -v
```

**Rebuild after code changes:**

```bash
docker compose up --build
```

The app exposes a health check at **`/api/health`** (returns `200` when MongoDB is reachable).

---

## Database options: local Mongo or MongoDB Atlas

By default Compose uses the bundled local MongoDB (`mongodb://mongo:27017/studyforge`).

To use **MongoDB Atlas** instead, add the connection string to `.env`:

```env
MONGO_URI=mongodb+srv://USER:PASS@CLUSTER0.xxxxx.mongodb.net/studyforge
```

The app then talks to Atlas and ignores the local Mongo container (which stays up as a fallback).

---

## Run locally without Docker

### 1. Start MongoDB

- **Docker (just the DB):**
  ```bash
  docker run -d -p 27017:27017 --name sf-mongo mongo:7
  ```
- **Atlas / remote:** set `MONGO_URI` in your environment (see reference).

### 2. Set environment variables

```bash
# PowerShell
$env:GEMINI_API_KEY = "AIza..."

# bash / zsh
export GEMINI_API_KEY="AIza..."
```

### 3. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The app listens on **http://localhost:8080** (or `PORT` if set).

---

## Run tests

```bash
cd backend
mvn test
```

Tests cover: progress calculation, AI plan parsing / validation / sanitisation, full plan→DTO mapping, complete/reopen semantics, status invariants (`TODO` default, sibling toggles don't flip statuses), id stability on update, and viewer/edit collaboration permissions.

To skip tests during a regular build:

```bash
mvn -DskipTests package
```

---

## Environment variable reference

| Variable | Default | Description |
|----------|---------|-------------|
| `GEMINI_API_KEY` | *(empty)* | **Required for AI features.** Google AI Studio key (`AIza…`). |
| `GEMINI_MODEL` | `gemini-3.6-flash` | Gemini model name. |
| `GEMINI_TIMEOUT_SECONDS` | `90` | HTTP timeout for Gemini calls. |
| `GEMINI_MAX_UPLOAD_BYTES` | `8388608` (8 MB) | Max PDF size accepted by the AI endpoint. |
| `MONGO_URI` | `mongodb://localhost:27017/studyforge` | Full MongoDB connection string (local Mongo or Atlas). |
| `JWT_SECRET` | *(insecure dev default)* | **Override before any production deploy.** Use a long random string. |
| `JWT_EXPIRATION_MS` | `604800000` (7 days) | JWT token lifetime in milliseconds. |
| `PORT` | `8080` | Port the Spring Boot app listens on inside the container. |
| `APP_PORT` | `8081` | Host-side port Docker Compose exposes. Only relevant when using Docker. |

---

## Troubleshooting

**Port already in use**

```
Error starting ApplicationContext: address already in use: 8080
```

Add `APP_PORT=9000` (Docker) or `PORT=9000` (local) to `.env` and restart.

**`Connection refused` to MongoDB**

Docker path — make sure both services are running:

```bash
docker compose ps
```

Local path — make sure `mongod` is running and listening on `27017`, or that `MONGO_URI` points at a reachable Atlas cluster.

**AI features return an error but manual mode works**

`GEMINI_API_KEY` is missing or invalid. Check the value in `.env`, confirm it starts with `AIza`, and restart the stack.

**Login fails after switching databases**

The auth and targets data live in the database named `studyforge`. If you previously pointed the app at a different database (e.g. `todoapp`), recreate the stack with the `MONGO_URI` correctly set and log in again:

```bash
docker compose up -d --force-recreate studyforge
```

---

## Project layout

```
ToDo/
├── .env.example            ← copy to .env and fill in your values
├── docker-compose.yml      ← optional local MongoDB + app services
└── backend/
    ├── Dockerfile          ← multi-stage build (Maven → JRE 21)
    ├── pom.xml
    ├── src/main/
    │   ├── java/com/studyforge/
    │   │   ├── controller/  Auth, Me, Target, Dashboard, Achievement, AI, Health
    │   │   ├── service/     Auth, Target, Notification, Replan, Progress, Achievement, Dashboard, AI plan, Gemini client
    │   │   ├── ai/          PlanParser — validates & sanitises all Gemini output
    │   │   ├── model/       Target (+Activity), Milestone, Task, Subtask, Member, User
    │   │   ├── repository/  Spring Data MongoDB repositories
    │   │   ├── dto/         Request/response records (including AI contracts)
    │   │   ├── security/    JWT util + filter, rate limiter
    │   │   ├── config/      Spring Security config + UserDetailsService
    │   │   └── exception/   ApiException + global handler
    │   └── resources/
    │       ├── application.yml
    │       └── static/      Vanilla HTML/CSS/JS frontend
    │           ├── index.html        Landing page
    │           ├── login.html / signup.html
    │           ├── dashboard.html    Today's actions, deadlines, search/filter/sort targets
    │           ├── create.html       AI + manual target wizard
    │           ├── target.html       Execution queue, AI re-plan, exports, activity log, sharing
    │           ├── achievements.html
    │           └── profile.html      Name + password management
    └── src/test/           Unit tests (plan parser, progress, target service)
```

---

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.3, Java 21, Spring Security + JWT |
| Database | MongoDB (local or MongoDB Atlas) |
| Frontend | Vanilla HTML / CSS / JS, served by Spring Boot |
| AI | Google Gemini REST API (text + PDF analysis) |
| Containerisation | Docker Compose (backend + optional local MongoDB) |

---

## Features

- Dark editorial landing page (purple palette)
- Email/password auth with JWT (JWT dark secret defaults, rate-limited login)
- Dashboard: today's actions, upcoming deadlines, progress rings, **searchable / filterable / sortable** target list
- **Manual mode** — full CRUD on targets, milestones, tasks, subtasks
- **AI mode**
  - Goal via text, paste, or PDF upload (Gemini reads the PDF)
  - Smart survey — only asks for genuinely missing info
  - Proposal editor — milestones, tasks, deadlines, priorities, member assignments all editable before saving
  - Regenerate, edit, accept — *AI suggests, you decide*
- **Execution loop** — overdue / up-next queue per target keeps the plan runnable
- **Re-plan with AI** — rebuild the schedule of an existing target on demand
- **Exports** — plan as **CSV**, **iCalendar (.ics)**, and **print-ready PDF** (browser print)
- **Collaboration v2** — invite teammates by email, **Editor/Viewer roles**, per-user permissions, activity log
- **Account page** — update name, change password
- **Notifications** — unread badge (shared-target activity) in the nav
- **Health endpoint** — `/api/health` for container health checks
- Deterministic progress calculation (never AI)
- Server-side achievements (subtle gamification)
- Graceful AI failure handling (bad JSON, empty plans, rate-limit errors → friendly messages)
- Tests :heavy_check_mark: 20 unit tests green

---

## AI principle

Gemini is used **only** where intelligence actually helps: understanding goals, reading PDFs, deciding what to ask, proposing plans. Everything deterministic — CRUD, auth, permissions, progress, deadlines, achievements — is plain application logic.

`PlanParser` treats all AI output as **untrusted input**: off-schema fields are dropped, invalid priorities/dates/offsets are clamped or defaulted, empty plans produce a friendly retry message. Proposals live in the client until the student explicitly accepts and saves them.