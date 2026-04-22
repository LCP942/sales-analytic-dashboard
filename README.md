# Sales Analytics Dashboard

![Angular](https://img.shields.io/badge/Angular-19-dd0031?logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6db33f?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479a1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-ready-2496ed?logo=docker&logoColor=white)
[![Live Demo](https://img.shields.io/badge/Live_Demo-Vercel-black?logo=vercel)](https://your-app.vercel.app)

A full-stack sales analytics dashboard built as a portfolio project for Angular freelance engagements.
The backend exposes five aggregation endpoints over a MySQL dataset of 500 seeded orders.
The frontend renders four interactive chart components driven by a reactive date-range filter.

> **Evaluators:** the live demo loads with real data — no login required.

---

## Screenshots

> *(Add 2–3 screenshots here after first deployment)*

---

## Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 19, standalone components, Signals, Chart.js |
| Backend | Spring Boot 3.3, Java 21 Records, virtual threads |
| Database | MySQL 8.4, 500 seeded orders (12 months, seasonal distribution) |
| Tests | `@DataJpaTest` (Spring), `HttpTestingController` (Angular) |
| Infra | Docker Compose, Railway (backend), Vercel (frontend) |

### Key architectural signals

- **`provideHttpClient(withFetch())`** in `app.config.ts` — modern fetch-based HTTP client
- **Angular Signals** (`signal()`, `computed()`, `effect()`) for filter state and reactive chart updates
- **Java 21 Records as DTOs** — instantiated directly via JPQL constructor expressions
- **`spring.threads.virtual.enabled=true`** — Project Loom virtual threads for high concurrency
- **Functional HTTP interceptor** — global loading indicator + `MatSnackBar` error banner
- **TypeScript strict mode** — zero `any` in production code

---

## Quick start (local)

**Prerequisites:** Docker Desktop

```bash
docker compose up --build
```

That's it. Once all three containers are healthy, open [http://localhost:4200](http://localhost:4200).

What happens under the hood:
- MySQL 8.4 starts and runs the schema + seed scripts (500 orders, 12 months of data)
- Spring Boot connects and starts on port `8080`
- Angular builds and is served by nginx on port `4200`

### Run tests

```bash
# Backend (requires Java 21)
cd backend && mvn test

# Frontend (requires Node.js 20+)
cd frontend && npm test
```

---

## Project structure

```
.
├── backend/                      # Spring Boot 3.3 / Java 21
│   └── src/main/java/.../
│       ├── entity/               # JPA entities (SalesOrder, Product, OrderItem)
│       ├── dto/                  # Java 21 Records (KpiRawDto, RevenuePointDto, …)
│       ├── repository/           # JPQL + native queries with @Param
│       ├── service/              # Aggregation logic, delta calculation, granularity
│       └── controller/           # 5 GET endpoints under /api/stats/
├── frontend/                     # Angular 19 SPA
│   └── src/app/
│       ├── core/                 # Models, LoadingService, FilterService, interceptor
│       ├── dashboard/            # DashboardComponent + 4 chart components + service
│       └── shared/               # DateRangeFilterComponent, SkeletonLoaderComponent
├── docker/init/                  # 01-schema.sql + 02-seed.sql
├── docker-compose.yml
└── railway.json
```

---

## API endpoints

All endpoints accept `?from=YYYY-MM-DD&to=YYYY-MM-DD`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/stats/kpis` | Revenue, orders, avg. order value + % delta vs previous period |
| GET | `/api/stats/revenue-over-time` | Revenue time series (auto granularity: day / week / month) |
| GET | `/api/stats/orders-over-time` | Order count time series (same granularity logic) |
| GET | `/api/stats/top-products` | Top 10 products by revenue |
| GET | `/api/stats/orders-by-category` | Item count per product category |

---

## Deployment

### Backend → Railway

1. Push this repository to GitHub.
2. Create a new Railway project → **Deploy from GitHub repo**.
3. Set the **Root Directory** to `/` (Railway reads `railway.json` which points to `backend/Dockerfile`).
4. Add a **MySQL** plugin in Railway and copy the connection variables.
5. Set the following environment variables in Railway:

   | Variable | Value |
   |---|---|
   | `DATABASE_URL` | `jdbc:mysql://<host>:<port>/railway` |
   | `DATABASE_USER` | from Railway MySQL plugin |
   | `DATABASE_PASSWORD` | from Railway MySQL plugin |
   | `CORS_ORIGINS` | your Vercel URL (e.g. `https://your-app.vercel.app`) |

6. On first deploy, Railway runs the Docker init scripts — the seed executes automatically.

> **Cold start tip:** configure UptimeRobot (free) to ping `GET /api/stats/kpis` every 5 minutes to prevent Railway's inactivity sleep.

### Frontend → Vercel

1. Update `frontend/src/environments/environment.prod.ts` with your Railway backend URL:
   ```ts
   export const environment = {
     production: true,
     apiBaseUrl: 'https://your-app.railway.app/api',
   };
   ```
2. Push the change.
3. Import the repo in Vercel → set **Root Directory** to `frontend`.
4. Vercel reads `vercel.json` automatically — no extra configuration needed.

---

## License

MIT
