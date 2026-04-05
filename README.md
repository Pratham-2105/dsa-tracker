# DSA Tracker Dashboard

A multi-user web platform for tracking every individual DSA problem solved across LeetCode, Codeforces, and CodeChef — at the per-problem level.

**Unlike Codolio**, this tracker logs every problem with full metadata (name, date, time taken, difficulty, tags, notes), lets you search and filter across platforms, and gives you actionable analytics for active placement prep — not just a portfolio showcase.

> 🚧 **Currently in active development.** Backend is being built thread by thread. Frontend coming soon.

---

## What Makes This Different

- Per-problem logging — not just aggregate counts
- Unified search: *"show me all Graph Mediums I solved 30+ days ago flagged for revision"*
- Calendar heatmap with streak tracking
- Topic gap analysis — see where you're weak
- Pattern Library — curated problem lists by algorithm pattern with progress tracking
- Contest history with unified rating progression across platforms
- Shareable profile cards via [lccard.dev](https://lccard.dev)

---

## Tech Stack

| Layer | Technology | Hosting |
|-------|-----------|---------|
| Backend | Spring Boot 3.2, Spring Security + JWT | Railway |
| Frontend | React (Vite + React Router), Recharts | Vercel |
| Database | PostgreSQL | Neon (free tier) |
| Cache | Redis | Upstash (free tier) |
| External APIs | Codeforces REST, LeetCode GraphQL | — |

---

## Project Status

| Thread | Scope | Status |
|--------|-------|--------|
| Thread 1 | Spring Boot setup, JWT auth, User entity | ✅ Done |
| Thread 2 | Problem + Tag CRUD | 🔲 Not started |
| Thread 3 | Codeforces API integration | 🔲 Not started |
| Thread 4 | LeetCode GraphQL integration | 🔲 Not started |
| Thread 5 | Async sync orchestration | 🔲 Not started |
| Thread 6 | Stats, heatmap, streaks, contests | 🔲 Not started |
| Thread 7 | React setup, landing page, auth pages | 🔲 Not started |
| Thread 8 | React dashboard | 🔲 Not started |
| Thread 9 | Problem log + Pattern Library pages | 🔲 Not started |
| Thread 10 | Verification flow, settings, deployment | 🔲 Not started |

---

## API Endpoints

### Auth (Thread 1 — Done)
```
POST   /api/auth/register     Register with email, password, optional platform handles
POST   /api/auth/login        Login → returns JWT
```

### Problems (Thread 2 — Coming)
```
GET    /api/problems          Filtered, paginated problem list
POST   /api/problems          Manual problem entry
PUT    /api/problems/{id}     Update notes, revision flag, etc.
DELETE /api/problems/{id}     Delete a problem
```

### Stats (Thread 6 — Coming)
```
GET    /api/stats/overview    Total counts by platform and difficulty
GET    /api/stats/tags        Problem count per tag/topic
GET    /api/stats/heatmap     Daily activity for heatmap
GET    /api/stats/streaks     Current streak, longest streak
```

### Sync (Thread 5 — Coming)
```
POST   /api/sync/{platform}   Trigger manual sync
GET    /api/sync/status       Current sync status
```

---

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL running locally
- Maven (bundled via `mvnw`)

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/YOUR_USERNAME/dsa-tracker.git
cd dsa-tracker
```

**2. Create the database**

Open pgAdmin and create a database called `dsatracker`.

**3. Configure `application.yml`**

Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dsatracker
    username: postgres
    password: YOUR_POSTGRES_PASSWORD
```

**4. Run**
```bash
./mvnw spring-boot:run
```

Or run `DsaTrackerApplication.java` directly from IntelliJ.

The app starts on `http://localhost:8080`. Hibernate auto-creates all tables on first run.

---

## Database Schema

### Users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    codeforces_handle VARCHAR(100),
    codeforces_verified BOOLEAN DEFAULT FALSE,
    leetcode_username VARCHAR(100),
    leetcode_verified BOOLEAN DEFAULT FALSE,
    codechef_username VARCHAR(100),
    codechef_verified BOOLEAN DEFAULT FALSE,
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);
```

> More tables (problems, tags, contests, daily_activity, patterns) will be added in Threads 2–6.

---

## Package Structure

```
src/main/java/com/dsatracker/dsa_tracker/
├── config/          SecurityConfig, CorsConfig, PasswordEncoderConfig
├── controller/      AuthController, (ProblemController, StatsController coming)
├── dto/             RegisterRequest, LoginRequest, LoginResponse
├── enums/           Platform, SyncStatus, (Difficulty, ProblemStatus coming)
├── exception/       GlobalExceptionHandler + custom exceptions
├── model/           User, (Problem, Tag, Contest coming)
├── repository/      UserRepository, (ProblemRepository coming)
├── security/        JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/         AuthService, (ProblemService, StatsService coming)
```

---

## Testing the API

Import into Postman or use curl:

**Register**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123","codeforcesHandle":"your_handle"}'
```

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123"}'
```

**Hit a protected endpoint**
```bash
curl http://localhost:8080/api/test/protected \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Contributing

This is an open source project. Once the core is stable (after Thread 6), contributions are welcome. Issues and PRs will be open.

---

## License

MIT
