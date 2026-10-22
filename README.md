# Paperless

A document-management study project built with Angular, Spring Boot, PostgreSQL, and RabbitMQ.
The current implementation supports user registration and login, document metadata management, search, and asynchronous notification of an OCR worker when a document is created.

## Components 

| Component | Technology |
| --- | --- |
| PaperlessWebUI | Angular 21, TypeScript, Tailwind CSS |
| Web server / reverse proxy | nginx |
| PaperlessREST | Java 25, Spring Boot 4.1.1 |
| Database | PostgreSQL 16, Spring Data JPA |
| Message broker | RabbitMQ |
| OCR worker (doesn't run on compose yet) | Java 25, Spring Boot AMQP |
| Object storage (planned) | MinIO  |
| Search engine (planned) | Elasticsearch (planned) |
| GenAI worker / provider (planned) | Gemini integration |


## Architecture!

### REST interface

All paths below use the host prefix `http://localhost/api`. Document endpoints require `Authorization: Bearer <token>`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/users` | Register with `username` and `password` |
| POST | `/sessions` | Log in and receive a JWT |
| POST | `/document` | Create metadata and trigger a document-saved event |
| GET | `/document` | List documents; optional `?query=...` searches title and OCR text |
| GET | `/document/{id}` | Read document metadata |
| PUT | `/document/{id}` | Replace editable metadata |
| DELETE | `/document/{id}` | Delete document metadata |
| GET / DELETE | `/document/{id}/data` | File operations; currently return 501 |

Example document request body:

```json
{
  "title": "Semester invoice",
  "type": "RECEIPT",
  "tags": ["university", "invoice"]
}
```

Supported document types are `RECEIPT`, `CONTRACT`, `BANK_STATEMENT`, `CERTIFICATE`, `NOTE`, `PASSPORT`, and `OTHER`.

## How to run:
Note: Host ports `80`, `5432`, `5672`, and `15672` must be available!

### 1. Configure the environment

Create a `.env` file beside `docker-compose.yml` with these local demonstration values:

```dotenv
SPRING_DATASOURCE_USERNAME=paperless
SPRING_DATASOURCE_PASSWORD=paperless_password
RABBIT_USER=paperless
RABBIT_PASSWORD=paperless_local_demo
RABBIT_VHOST=paperless
JWT_SECRET=REPLACE_WITH_BASE64_KEY
```

The database credentials above must match the PostgreSQL service configuration. 
Generate a random JWT signing key, then replace the placeholder with the printed value:

### 2. Build the API JAR

The API Dockerfile copies a prebuilt JAR; it does not run Maven inside the image build.

```powershell
cd api
.\mvnw.cmd clean package -DskipTests
cd ..
```

This must produce `api/target/api-0.0.1-SNAPSHOT.jar`.
The frontend is built automatically inside its Docker build, so local Node.js is not required for this Compose workflow.

### 3. Start infrastructure, then the application

```powershell
docker compose up -d postgres paperless-mq
docker compose ps
docker compose logs postgres paperless-mq
```

Wait until PostgreSQL is ready to accept connections and RabbitMQ reports startup completion. The current Compose file has no readiness health checks.

```powershell
docker compose up -d --build paperless-rest paperless-web-ui
docker compose logs -f paperless-rest
```

Wait for the API startup message. Ctrl+C exits log following without stopping the containers. Hibernate creates or updates the schema through `spring.jpa.hibernate.ddl-auto=update`; `api/init.sql` is not mounted by Compose.

### 4. Start the OCR worker separately

In a second terminal, starting at the project root:

```powershell
cd ocr-worker
.\mvnw.cmd spring-boot:run
```

The worker reads the root `.env` through its Spring configuration and connects to RabbitMQ at `localhost:5672`.
The worker currently has no Dockerfile or Compose service.

### 5. Open and use the application

| Service | Address |
| --- | --- |
| Web application | http://localhost |
| REST API through nginx | http://localhost/api |
| RabbitMQ management UI | http://localhost:15672 |


### How to stop the application:

```powershell
docker compose down
```

## Testing strategy
### Existing tests

| Level | Tools | Coverage |
| --- | --- | --- |
| Service unit tests | JUnit, Mockito | Metadata defaults and type creation, event publication on save, no publication when repository save fails, and update behavior. |
| Controller tests | Spring MVC test slices, MockMvc, Mockito | Registration/login happy paths, document listing/search delegation, metadata mapping, creation/update, blank-title validation, and missing-document responses. |
| Frontend tests | Angular TestBed, Vitest | Duplicate-tag validation and generated application tests. |

Next planned tests are Integration Tests for RabbitMQ!

Run the worker tests from `ocr-worker/` or `api/` with RabbitMQ available:

```powershell
.\mvnw.cmd test
```

For frontend development/testing, install Node.js 24 and run from `frontend/`:

```powershell
npm ci
npm test -- --watch=false
```

## Lessons learned
- Think of logging messages early and design them in a way they are easily traceable.
- Think of tests early!