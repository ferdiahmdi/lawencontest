# Plan: Employee Leave API Design

Design a minimal Spring Boot REST API with PostgreSQL that supports leave requests, approvals, and validation rules, then document schema, contract, and app structure aligned with JWT roles plus a role field. This plan focuses on a simple, extensible model using LocalDate and Spring Data pagination.

**Steps**

1. Define core entities and constraints for PostgreSQL: employees (with role), leaves (with status, dates), and optional yearly quota table; include indexes and constraints for overlap and quota validation.
2. Draft API contract for employee and manager flows: apply leave, view own history, manager list all, approve/reject; include auth/login flow to obtain JWT, request/response shapes, error model, pagination, and validation errors.
3. Specify application structure: packages for controller/service/repository/domain/dto/security/exception/config; define services for validation (quota/overlap) and transaction boundaries.
4. Add optional OpenAPI and migration plan: outline Flyway/Liquibase baseline scripts and OpenAPI annotations/config to generate docs.
5. Add non-functional scaffolding: global exception handling, transactional service methods, and pagination/sorting defaults.

**Verification**

1. Validate API contract coverage against requirements (auth/token, apply, own history, manager list, approve/reject, quota, overlap).
2. Verify schema supports required queries (overlap detection, per-employee listing, status transitions).
3. Check pagination and error model consistency across endpoints.
4. Confirm security model supports JWT roles plus persisted role field.

**Decisions**

- Auth: JWT roles are the source of truth; also store role on Employee for display/audit only.
- Auth token: provide a simple local login flow to obtain a JWT for local/dev usage.
- Auth storage: store a bcrypt `password_hash` on Employee; never store plaintext.
- Leave unit: full days only using LocalDate.
- Overlap: block pending and approved.
- Statuses: PENDING/APPROVED/REJECTED.
- Pagination: page/size + sort (Spring Page).
- Quota: fixed yearly quota of 15 days for all employees.
- Roles: two roles only (EMPLOYEE, MANAGER); managers do not submit their own leave requests in this scope.

**Further Considerations**

1. If role in JWT and DB differ, JWT is the source of truth; DB role is display/audit only.
2. Yearly quota is fixed at 15 days; decide whether to keep in config only or also persist in a quota table.
3. Manager self-leave requests are out of scope; only EMPLOYEE and MANAGER roles exist with no hierarchy.

## Designs

### Schema (PostgreSQL)

Minimal tables: `employees`, `leaves`, optional `leave_quotas`.

**employees**

- `id` UUID PK
- `email` TEXT UNIQUE NOT NULL
- `full_name` TEXT NOT NULL
- `role` TEXT NOT NULL CHECK (role IN ('EMPLOYEE','MANAGER'))
- `password_hash` TEXT NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()

**leaves**

- `id` UUID PK
- `employee_id` UUID NOT NULL REFERENCES employees(id)
- `status` TEXT NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED'))
- `start_date` DATE NOT NULL
- `end_date` DATE NOT NULL
- `days` INTEGER NOT NULL CHECK (days > 0)
- `reason` TEXT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()

**leave_quotas** (optional)

- `id` UUID PK
- `employee_id` UUID NOT NULL REFERENCES employees(id)
- `year` INTEGER NOT NULL
- `quota_days` INTEGER NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- UNIQUE (`employee_id`, `year`)

**Indexes/constraints**

- Index `leaves_employee_id_start_date` on (`employee_id`, `start_date`)
- Index `leaves_status` on (`status`)
- For overlap blocking on pending/approved, prefer a service-layer check with a supporting index. If you want DB-level protection, use a `daterange` exclusion constraint with `gist` (requires `btree_gist` extension), scoped to `status IN ('PENDING','APPROVED')`.

**Validation rules**

- `start_date <= end_date`.
- Leaves must be within a single calendar year (simple quota accounting).
- Overlap is blocked for PENDING and APPROVED for the same employee.
- Quota per year is 15 days (fixed); if `leave_quotas` is used, default to 15 when absent.

### API Contract

Base path: `/api`.

**Employee**

- `POST /auth/login`
  - Request:
    ```json
    {
      "email": "employee@company.com",
      "password": "password"
    }
    ```
  - Response 200:
    ```json
    {
      "token": "jwt",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
    ```

- `POST /leaves`
  - Request:
    ```json
    {
      "startDate": "2026-06-10",
      "endDate": "2026-06-12",
      "reason": "Family event"
    }
    ```
  - Response 201:
    ```json
    {
      "id": "uuid",
      "employeeId": "uuid",
      "status": "PENDING",
      "startDate": "2026-06-10",
      "endDate": "2026-06-12",
      "days": 3,
      "reason": "Family event",
      "createdAt": "2026-05-22T08:15:30Z"
    }
    ```

- `GET /leaves`
  - Query: `page`, `size`, `sort`, optional `status`, `from`, `to`
  - Response 200: Spring Page wrapper

**Manager**

- `GET /manager/leaves`
  - Query: `page`, `size`, `sort`, optional `status`, `employeeId`, `from`, `to`
  - Response 200: Spring Page wrapper

- `POST /manager/leaves/{id}/approve`
- `POST /manager/leaves/{id}/reject`
  - Request:
    ```json
    { "note": "Optional manager note" }
    ```
  - Response 200: same shape as leave item

**Errors**

- 400 validation:
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request",
    "fieldErrors": [
      { "field": "startDate", "message": "must be on or before endDate" }
    ]
  }
  ```
- 403 for forbidden role
- 404 when leave not found
- 409 for overlap or quota exceeded
- 401 for invalid credentials or missing/invalid token

### Application Structure

- `controller` (REST endpoints)
- `service` (business logic, quota + overlap checks, status transitions)
- `repository` (Spring Data JPA)
- `domain` (entities)
- `dto` (request/response models)
- `security` (JWT auth, role mapping, method security)
- `exception` (custom exceptions, global handler)
- `config` (pagination defaults, OpenAPI if enabled)

**Security**

- JWT role is the source of truth for authorization.
- `employees.role` is persisted for display/audit only.
- `password_hash` uses BCrypt; login validates email/password and issues JWT.

### OpenAPI and Migrations (Optional)

- Use Flyway or Liquibase for baseline schema.
- Add OpenAPI annotations or Springdoc to generate docs.

### Non-Functional

- Global exception handler for consistent error responses.
- Transactional service methods for approve/reject and create leave.
- Pagination defaults (e.g., `page=0`, `size=20`, `sort=createdAt,desc`).
