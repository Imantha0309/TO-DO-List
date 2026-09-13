# StudyForge

**An intelligent academic target & project planning assistant for university students.**

> Say the goal → AI understands it → AI asks only what's missing → AI proposes a plan → you review, edit, and execute.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment variables](#environment-variables)
3. [Run with Docker (recommended)](#run-with-docker-recommended)
4. [Run locally without Docker](#run-locally-without-docker)
5. [Run tests](#run-tests)
6. [Environment variable reference](#environment-variable-reference)
7. [Project layout](#project-layout)
8. [Stack](#stack)
9. [Features](#features)
10. [AI principle](#ai-principle)

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| **Docker Desktop** | 24+ | Required for the Docker path |
| **Docker Compose** | v2 (`docker compose`) | Bundled with Docker Desktop |
| **Java** | 21 | Required only for the local (no-Docker) path |
| **Maven** | 3.9+ | Required only for the local path; or use the Maven wrapper if present |
| **MongoDB** | 7 | Required only for the local path (Docker spins one up automatically) |
| **Gemini API key** | — | Free tier at [aistudio.google.com/apikey](https://aistudio.google.com/apikey) — starts with `AIza`. AI features show a friendly error without one; manual mode works fine. |

---

## Environment variables

The project ships with `.env.example`. Copy it to `.env` before you start:

```bash
cp .env.example .env
```

Open `.env` and set at minimum:

```env
GEMINI_API_KEY=AIza...   # your real Google AI Studio key
```

> `.env` is in `.gitignore` and is never committed.

---

## Run with Docker (recommended)

This is the fastest path — no Java or MongoDB installation needed.

```bash
# 1. Clone the repo (skip if you already have it)
git clone <repo-url>
cd ToDo

# 2. Copy and fill the env file
cp .env.example .env
#    edit .env — set GEMINI_API_KEY=AIza...

# 3. Build and start
docker compose up --build
```

Docker Compose will:
- Pull **MongoDB 7** and start it at `localhost:27017`
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

---

## Run locally without Docker

Use this path when you want faster iteration (no image rebuild on every change).

### 1. Start MongoDB

Make sure a MongoDB 7 instance is running on `localhost:27017`.

- **Docker (just the DB):**
  ```bash
  docker run -d -p 27017:27017 --name sf-mongo mongo:7
  ```
- **Local install:** start `mongod` normally.
- **Atlas / remote:** set `MONGO_URI` in your environment (see [reference](#environment-variable-reference)).

### 2. Set environment variables

Either export them in your shell:

```bash
# PowerShell
$env:GEMINI_API_KEY = "AIza..."

# bash / zsh
export GEMINI_API_KEY="AIza..."
```

Or keep a `.env` file at the repo root — Spring Boot does **not** read `.env` automatically, so you need to source it yourself or use your IDE's run config.

### 3. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The app starts on **http://localhost:8080** (port `8080` directly, no Docker mapping).

---

## Run tests

```bash
cd backend
mvn test
```

Tests cover: progress calculation, AI plan parsing / validation / sanitisation, target mapping, JSON date serialisation, and complete/reopen semantics.

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
| `MONGO_URI` | `mongodb://localhost:27017/studyforge` | Full MongoDB connection string. |
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

Change the host port: add `APP_PORT=9000` to `.env` and restart.

---

**`Connection refused` to MongoDB**

Docker path — make sure both services are running:

```bash
docker compose ps
```

Local path — make sure `mongod` is running and listening on `27017`.

---

**AI features return an error but manual mode works**

`GEMINI_API_KEY` is missing or invalid. Check the value in `.env`, confirm it starts with `AIza`, and restart the stack.

---

**`docker compose` not found**

You may have the older standalone `docker-compose` (v1). Use `docker-compose up --build` instead, or upgrade Docker Desktop.

---

## Project layout

```
ToDo/
├── .env.example            ← copy to .env and fill in your values
├── docker-compose.yml      ← MongoDB + app services
└── backend/
    ├── Dockerfile           ← multi-stage build (Maven → JRE 21)
    ├── pom.xml
    └── src/main/
        ├── java/com/studyforge/
        │   ├── controller/  Auth, Target, Dashboard, Achievement, AI
        │   ├── service/     Auth, Target, Progress, Achievement, Dashboard, AI plan, Gemini client
        │   ├── ai/          PlanParser — validates & sanitises all Gemini output
        │   ├── model/       Target, Milestone, Task, Subtask, Member, User
        │   ├── repository/  Spring Data MongoDB repositories
        │   ├── dto/         Request/response records (including AI contracts)
        │   ├── security/    JWT util + filter
        │   ├── config/      Spring Security config
        │   └── exception/   ApiException + global handler
        └── resources/
            ├── application.yml
            └── static/      Vanilla HTML/CSS/JS frontend
                ├── index.html        Landing page
                ├── login.html
                ├── signup.html
                ├── dashboard.html
                ├── create.html       AI + manual target wizard
                ├── target.html       Target detail / task tracker
                └── achievements.html
```

---

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.3, Java 21, Spring Security + JWT |
| Database | MongoDB 7 |
| Frontend | Vanilla HTML / CSS / JS, served by Spring Boot |
| AI | Google Gemini REST API (text + PDF analysis) |
| Containerisation | Docker Compose (backend + MongoDB) |

---

## Features

- Dark editorial landing page (purple palette, X.company-inspired)
- Email/password auth with JWT
- Dashboard: today's actions, upcoming deadlines, progress rings, recent targets
- **Manual mode** — full CRUD on targets, milestones, tasks, subtasks
- **AI mode**
  - Goal via text, paste, or PDF upload (Gemini reads the PDF)
  - Smart survey — only asks for genuinely missing info
  - Proposal editor — milestones, tasks, deadlines, priorities, member assignments all editable before saving
  - Regenerate, edit, accept — *AI suggests, you decide*
- Deterministic progress calculation (never AI)
- Server-side achievements (subtle gamification)
- Graceful AI failure handling (bad JSON, empty plans, rate-limit errors → friendly messages)

---

## AI principle

Gemini is used **only** where intelligence actually helps: understanding goals, reading PDFs, deciding what to ask, proposing plans. Everything deterministic — CRUD, auth, permissions, progress, deadlines, achievements — is plain application logic.

`PlanParser` treats all AI output as **untrusted input**: off-schema fields are dropped, invalid priorities/dates/offsets are clamped or defaulted, empty plans produce a friendly retry message. Proposals live in the client until the student explicitly accepts and saves them.

---

*StudyForge · Designed & built by **ayyubidlk***
#   T O - D O - L i s t  
 