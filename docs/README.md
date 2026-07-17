# ForecastPro

**Sales Analysis and Demand Forecasting System** — Jakarta EE 10 web application (MSc project / enterprise demo).

## Stack

| Layer | Technology |
|--------|------------|
| Web / UI | JSF 4 (Facelets), PrimeFaces 13 (Jakarta) |
| Business | EJB 4 stateless session beans |
| Persistence | JPA 3 (EclipseLink on Payara), MySQL 8 (JTA datasource) |
| Security | Session-based login, servlet filter + EJB role checks |
| API | JAX-RS (REST) under `/api` |
| Build | Maven, Java 17 |
| Target server | **Payara Server Community** (Jakarta EE 10) |

**Display:** monetary amounts use **INR (₹)** in the UI; dates are shown as **dd/MM/yyyy** (monthly analytics labels use the first day of each month in that format).

## Roles

| Role | Capabilities |
|------|----------------|
| **ADMIN** | Users, categories, products, sales, analytics, forecast, alerts |
| **SALES_MANAGER** | Dashboard, analytics, forecast, alerts (no product CRUD, no sales entry, no user management) |
| **EMPLOYEE** | Products, sales entry (no analytics, forecast, alerts, user management) |

URL patterns are enforced in `com.forecastpro.config.SecurityFilter`; business operations are validated again in EJBs (`SecurityService`).

## Prerequisites

- **JDK 17+**
- **Apache Maven 3.9+**
- **MySQL 8** (or compatible)
- **Payara Server Community 6.x** (Jakarta EE 10) — e.g. deploy from **NetBeans** Services → **Servers** → Payara

## Database

1. Create a database (name must match your JDBC URL, e.g. `forecastpro`):

   ```sql
   CREATE DATABASE forecastpro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Create the tables** (pick one approach):

   **A — Manual (most reliable)**  
   Execute the full script **`db/schema.sql`** in MySQL (Workbench, `mysql` CLI, or NetBeans). That creates `users`, `categories`, `products`, `sales`, and `forecasts`. Then redeploy the app (or just restart). Demo data still comes from `DataSeedService` when the `users` table is empty.

   **Table name:** use **`users`** (plural). `SELECT * FROM user` will fail — there is no `user` table.

   **Empty tables:** run **`db/seed-all.sql`** to load users, categories, products, sales, and sample forecasts in one go (matches `DataSeedService`). For **users only**, use **`db/seed-users.sql`**. If rows already exist, use the optional `TRUNCATE` block at the top of `seed-all.sql` or insert manually without duplicates.

   **B — Let JPA create them on deploy**  
   **Payara uses EclipseLink**, not Hibernate. `persistence.xml` sets `eclipselink.ddl-generation` = `create-or-extend-tables` so tables *should* appear after a clean deploy. If they still do not, use **A** and ignore this.

   Earlier configs used Hibernate’s `update`, which EclipseLink **ignores** — that is why you may have seen **no tables** until now.

### Production note

After the schema is stable, set `eclipselink.ddl-generation` to `none` in `persistence.xml` and manage schema with SQL migrations or DBA tools.

3. Configure a **JTA datasource** on Payara that the application resolves as:

   **`jdbc/ForecastProDS`**

   This matches `persistence.xml` (`<jta-data-source>jdbc/ForecastProDS</jta-data-source>`).

### Payara — JDBC Connection Pool and JDBC Resource (important)

**Do not use JBoss/WildFly JNDI names** (`java:jboss/datasources/...`). On Payara you create a pool and bind a portable JDBC resource.

1. Copy the **MySQL Connector/J** JAR into Payara (e.g. `$PAYARA_HOME/glassfish/lib/`) or register it as a library per Payara documentation.
2. Start Payara (domain: typically `domain1`, HTTP port **8080** by default).
3. Open the **Admin Console** (e.g. `http://localhost:4848`).
4. **Resources → JDBC → JDBC Connection Pools** → **New**  
   - Pool name: e.g. `ForecastProPool`  
   - Resource type: `javax.sql.DataSource`  
   - Database driver: MySQL  
   - Set properties: URL (`jdbc:mysql://localhost:3306/forecastpro?serverTimezone=UTC&useSSL=false`), user, password, etc.
5. **Ping** the pool to verify connectivity.
6. **Resources → JDBC → JDBC Resources** → **New**  
   - **JNDI name:** `jdbc/ForecastProDS`  
   - **Pool name:** `ForecastProPool`

The JDBC resource JNDI name **`jdbc/ForecastProDS`** matches what this application uses in `persistence.xml`.

7. Restart the server if required, then deploy `forecastpro.war`.

**NetBeans:** register Payara in **Services → Servers**, add the project server, and deploy; ensure the JDBC resource exists on that Payara domain before first run.

## Build

```bash
cd ForecastPro
mvn clean package
```

Artifact: `target/forecastpro.war`

## Deploy

1. Start **Payara Server**.
2. Deploy `forecastpro.war` (asadmin, admin console, or NetBeans **Run**).
3. Context root is **`/forecastpro`** (see `src/main/webapp/WEB-INF/glassfish-web.xml`).

### Application URL

- UI login: `http://localhost:8080/forecastpro/login.xhtml`
- Health (servlet): `http://localhost:8080/forecastpro/health`
- REST examples:
  - `GET http://localhost:8080/forecastpro/api/products`
  - `GET http://localhost:8080/forecastpro/api/forecasts/recent`

REST endpoints are open for demo purposes; protect them (e.g. container security or filters) in production.

## First run and demo users

On **first deployment** with an **empty `users` table**, `DataSeedStartup` (`@Singleton` `@Startup`) invokes `DataSeedService.seedIfEmpty()` and creates:

| Username   | Password     | Role           |
|-----------|--------------|----------------|
| admin     | Admin@123    | ADMIN          |
| manager   | Manager@123  | SALES_MANAGER  |
| employee  | Employee@123 | EMPLOYEE       |

It also seeds three categories (Furniture, Home Appliances, Electronics), sample products, and several months of sales so analytics and forecasting work immediately.

Optional notes are in `db/sample-data.sql`.

### If deployment failed with `DataSeedStartup` / seed errors

`DataSeedStartup` catches seed failures so Payara still deploys. Check **server.log** for `ForecastPro: automatic data seed failed`. Ensure **MySQL** is up and **`jdbc/ForecastProDS`** is valid; you can still load **`db/seed-all.sql`** manually.

## Forecasting logic

Implemented in `ForecastService`:

- **Moving average** — last 3 months of monthly quantities (`MovingAverageForecaster`).
- **Regression** — Apache Commons Math `SimpleRegression` on month index vs quantity; next step predicts at `x = n` (same series as the moving average input).

**Predicted units** = average of moving average and regression prediction. **Predicted revenue** = predicted units × product price.

Stored in `forecasts` (`moving_avg_value`, `ml_regression_value`, `predicted_sales`, `predicted_revenue`).

**Inventory alerts** compare **predicted sales** (ensemble) to current stock and suggest restock when demand exceeds stock.

## Project layout (packages)

```
com.forecastpro.controller   — JSF @Named beans, session
com.forecastpro.service      — EJB facades + security
com.forecastpro.repository   — JPA data access
com.forecastpro.entity       — JPA entities
com.forecastpro.dto          — Row/DTO types for views and REST
com.forecastpro.ml           — Moving average helper (regression via Commons Math in `ForecastService`)
com.forecastpro.util         — Password hashing (BCrypt), validation
com.forecastpro.config       — Filters, exception handler, JAX-RS app, bootstrap, servlet
com.forecastpro.rest         — JAX-RS resources
```

## Viva / demo checklist

1. Log in as **admin**, **manager**, and **employee** and show menu differences.
2. Show **category → product** flow on Products and **category → product** on Forecast.
3. Record a **sale** and show stock decreasing.
4. Open **Analytics** (admin/manager) and explain SQL aggregation vs Java summaries.
5. Run **Generate forecast** and show stored rows; open **Inventory alerts** after forecasts exist.
6. Mention **layered architecture**, **JPA**, **EJB transactions**, and **manual ML** (linear regression).

## Licence

Academic / demonstration project — adapt as needed for your institution’s policies.
