# Sales Analytics Dashboard

### [→ Live demo](https://sales-analytic-dashboard.lcp942.com)
> Auto-generated data — no setup, no account required.

---

![Angular](https://img.shields.io/badge/Angular-21-dd0031?logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6db33f?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479a1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-ready-2496ed?logo=docker&logoColor=white)

A full-stack sales analytics dashboard.
The backend exposes aggregation and CRUD endpoints over a self-managed MySQL dataset.
The frontend renders interactive chart components driven by a reactive date-range filter.

---

## Screenshots

![img_2.png](docs/img_2.png)
![img_4.png](docs/img_4.png)
![img_5.png](docs/img_5.png)

---

## Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 21, Angular Material, Tailwind CSS, ECharts |
| Backend | Spring Boot 3.3, Java 21, virtual threads |
| Database | MySQL 8.4 |
| Tests | JUnit 5 + MockMvc (backend), Jasmine + Karma (frontend) |
| Infra | Docker Compose (local), any container host (production) |

---

## Quick start (local)

**Prerequisites:** Docker Desktop

```bash
docker compose up --build
```

Once all three containers are healthy, open [http://localhost:4200](http://localhost:4200).

The app seeds its own data on first boot — no manual SQL import needed.

### Run tests

```bash
# Backend (requires Java 21)
cd backend && mvn test

# Frontend (requires Node.js 20+)
cd frontend && npm test
```

---

## API endpoints

All stats endpoints accept `?from=YYYY-MM-DD&to=YYYY-MM-DD`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/stats/kpis` | Revenue, orders, avg. order value + % delta vs previous period |
| GET | `/api/stats/revenue-over-time` | Revenue time series (auto granularity: day / week / month) |
| GET | `/api/stats/orders-over-time` | Order count time series |
| GET | `/api/stats/top-products` | Top 10 products by revenue |
| GET | `/api/stats/orders-by-category` | Item count per product category |
| GET | `/api/orders` | Paginated order list with filters |
| GET | `/api/orders/{id}` | Order detail with items |
| POST | `/api/orders` | Create an order |
| GET | `/api/customers` | Paginated customer list |
| GET | `/api/customers/{id}` | Customer detail |
| POST | `/api/customers` | Create a customer |

---

## Deployment

The app has two independent deployable units.

### Backend

A Spring Boot JAR packaged as a Docker image (`backend/Dockerfile`).
Requires a **MySQL 8.4** database — the schema is created automatically on first boot.

**Environment variables:**

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | yes | JDBC URL — `jdbc:mysql://<host>:<port>/sales_dashboard` |
| `DATABASE_USER` | yes | Database username |
| `DATABASE_PASSWORD` | yes | Database password |
| `CORS_ORIGINS` | yes | Comma-separated list of allowed frontend origins |
| `SPRING_PROFILES_ACTIVE` | no | Set to `prod` to enable JSON structured logging (default: human-readable text) |

### Frontend

A static Angular SPA built with `npm run build`.

Set the backend URL before building:

```ts
// frontend/src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://your-backend-url/api',
};
```

Requires a catch-all rewrite so all paths serve `index.html`. Example for nginx:

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

---

## License

MIT
