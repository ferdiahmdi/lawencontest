# Leave Service API

A Spring Boot REST API for employee leave requests and manager approvals. It uses PostgreSQL, JPA, and Liquibase migrations.

## Project Highlights

- Spring Boot 4.0.6, Java 25
- JWT-based auth with roles claim (EMPLOYEE, MANAGER) -- only needed to get JWT tokens
- Leave validation (date range, same-year, overlap, quota)
- Liquibase migrations and optional seed data
- OpenAPI UI via Springdoc

## Tech Stack

- Java 25
- Spring Boot (WebMVC, Security, Data JPA, Validation)
- PostgreSQL
- Liquibase
- Springdoc OpenAPI

## Prerequisites

- Java 25
- Maven (or use ./mvnw)
- PostgreSQL 18+

## Quick Start

1. Create a PostgreSQL database:

```sql
CREATE DATABASE leave_service;
```

2. Configure the database and JWT secret in `src/main/resources/application.yaml` or override via environment variables.

Current defaults:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/leave_service
    username: postgres
    password: admin

app:
  security:
    jwt:
      issuer: leave-service
      expiration-seconds: 3600
      secret: iniSecretYangDipushKeGitHubKarenaKenapaTidak
```

3. Run the app:

```sh
./mvnw
```

The service listens on port 8000.

## Authentication

- Login endpoint: `POST /api/auth/login`
- JWT is signed with HS256.
- Roles are included in the `roles` claim and mapped to `ROLE_` authorities.

### Login Request

```json
{
  "username": "JaneEmployee",
  "password": "password"
}
```

### Login Response

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer"
}
```

### Using the Token

```http
Authorization: Bearer <jwt>
```

## API Endpoints

Base path: `/api`

### Auth

- `POST /auth/login`

### Employee

- `POST /leaves` (create a leave request)
- `GET /leaves` (list own leaves)

(ROLE_MANAGER)

- `GET /leaves` (list all leaves)
- `POST /leaves/{id}/approve`
- `POST /leaves/{id}/reject`

Query params for `GET /leaves`:

- `status`: PENDING | APPROVED | REJECTED
- `from`: yyyy-MM-dd
- `to`: yyyy-MM-dd
- `page`, `size`, `sort` (Spring Page)

Query params for `GET /leaves`:

- `employeeId`: UUID
- `status`: PENDING | APPROVED | REJECTED
- `from`: yyyy-MM-dd
- `to`: yyyy-MM-dd
- `page`, `size`, `sort`

## Validation and Business Rules

- `startDate` must be on or before `endDate`.
- Leave requests must be within a single calendar year.
- Overlaps are blocked for PENDING and APPROVED leaves.
- Quota is fixed at 15 days per year (stored on employee record).

## Error Response Shape

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "fieldErrors": [{ "field": "startDate", "message": "must not be null" }]
}
```

Notes:

- `fieldErrors` is omitted when empty.
- Common codes: `VALIDATION_ERROR`, `REQUEST_ERROR`, `BAD_REQUEST`, `METHOD_NOT_ALLOWED`,
  `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL_ERROR`.

## Database Schema

Liquibase changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`

Tables:

- `employees`
  - `id` (uuid, PK)
  - `username` (unique)
  - `password_hash`
  - `role` (EMPLOYEE | MANAGER)
  - `quota` (default 15)
  - `created_at`

- `leaves`
  - `id` (uuid, PK)
  - `employee_id` (FK employees)
  - `approved_by` (FK employees, nullable)
  - `status` (PENDING | APPROVED | REJECTED)
  - `start_date`, `end_date`
  - `reason`
  - `created_at`, `updated_at`

## Seed Data

Liquibase context `seed` inserts sample employees. Password hashes are empty by default.

To disable seed data, remove `seed` from:

```yaml
spring:
  liquibase:
    contexts: seed
```

## OpenAPI

<!-- - Swagger UI: `/swagger-ui/index.html` -->

- OpenAPI JSON: `/v3/api-docs`

## Common Dev Tasks

- Run app: `./mvnw`
- Run tests: `./mvnw test`

## Project Structure

```
src/main/java/com/lawencon/leave_service
  controller
  service
  repository
  domain
  dto
  security
  exception
  config
```

## Notes

- JWT issuer and secret are configured in `application.yaml`.
- This project uses `com.lawencon.leave_service` as the base package.
