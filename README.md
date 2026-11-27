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
| OCR worker | Java 25, Spring Boot AMQP |
| Object storage | Silo (`pgsty/silo`, MinIO-compatible fork), MinIO Java client |
| Search engine | Elasticsearch 9.4, `elasticsearch-java` client |
| GenAI worker / provider (planned) | Gemini integration |


## Architecture

### Document processing flow

```
Upload ─► REST: save metadata in PostgreSQL ─► store PDF in Silo ─► index title in Elasticsearch
            └─► after commit: DocumentSavedEvent ─► exchange document.events
                  └─► OCR worker: load PDF from Silo, run Tesseract
                        └─► DocumentOcrCompletedEvent ─► exchange ocr.events
                              └─► REST result listener: add OCR text to the Elasticsearch entry
Search ─► REST ─► Elasticsearch (ranked document IDs) ─► load documents from PostgreSQL
```

PostgreSQL is the source of truth; Elasticsearch is only the search view. The REST service owns the `title` in the index (upload, rename, delete); its OCR-result listener adds `text`.

Search index behavior of the REST service:
- On startup it creates the index `documents` with an explicit mapping (`title`: text + keyword sub-field, `text`: text, `dynamic: strict`) if it does not exist yet.
- Title changes (upload, rename) and deletions are written to Elasticsearch only **after** the database transaction has committed, so a rollback never leaves an orphaned entry. A failing Elasticsearch call is logged and does not block the database change.
- Renaming writes the title only when it actually changed.

### Shared names

All services use these names and the same `.env` variables:

| What | Name |
| --- | --- |
| Storage bucket / object key | `documents` / document ID (never the file name) |
| Storage variables | `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |
| Upload event | exchange `document.events` (fanout) → queue `ocr.document.events`, payload `{ documentId, documentName }` |
| OCR result event | exchange `ocr.events` (fanout) → queue `api.ocr.events`, payload `{ documentId, documentName, content }` |
| Summary event | exchange `genai.events` (fanout) → queue `api.genai.events`, payload `{ documentId, summary }` |
| Search index | `documents`, fields `title`, `text` and `summary`, Elasticsearch `_id` = document ID |

### REST interface

The API declares `document.events` at startup via `RabbitAdmin.initialize()`. It also provisions the durable OCR job queue and binding so jobs can wait while the worker is offline. The worker does not declare this exchange. Docker Compose starts the worker after the API is ready; `/health` returns 503 until application startup has completed.

All paths below use the host prefix `http://localhost/api`. Document endpoints require `Authorization: Bearer <token>`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/users` | Register with `username` and `password` |
| POST | `/sessions` | Log in and receive a JWT |
| POST | `/document` | Create metadata and trigger a document-saved event |
| POST | `/document` (multipart) | Upload a PDF (`file`, `title`, `type`, `tags`): saves metadata, then stores the PDF in Silo, then triggers the event |
| GET | `/document` | List documents; optional `?query=...` searches title and OCR text |
| GET | `/document/{id}` | Read document metadata |
| PUT | `/document/{id}` | Replace editable metadata |
| DELETE | `/document/{id}` | Delete the document and its stored PDF |
| GET | `/document/{id}/data` | Download the stored PDF (`404` if the document or its file does not exist) |
| GET | `/document/{id}/summary` | Read the generated summary: `200 { "summary": "..." }`, `204` while it does not exist yet, `404` for an unknown document, `503` if the search index is unavailable |

If the storage is unavailable, the upload fails with `503` and nothing is persisted (the database transaction is rolled back). Uploads are limited to 15 MB.

Summaries live only in Elasticsearch (field `summary`, stored but not searchable), not in PostgreSQL: we do not know how long a summary gets. The API writes it when the summary event arrives and reads it through `GET /document/{id}/summary`. The detail page shows it in the AI tab and asks again every 3 seconds (at most 20 times) while the endpoint answers `204`; after that the user can check again manually.

Known limitation: the summary event only reports success. If the GenAI call fails for good, no event is sent, so the API cannot tell "still being generated" from "failed" and answers `204` in both cases. The UI therefore says "No summary available yet" after the waiting time. Telling these cases apart would need a failure event from the GenAI worker, which we deliberately did not add to the contract.

Both document creation endpoints return `201 Created` with a `Location` header. Listing documents returns `200` with results, or `204 No Content` when there are none (including searches without matches).

The API validates JSON and multipart document requests: titles must be nonblank and at most 200 characters; at most 20 tags are allowed, each nonblank, without whitespace, and at most 50 characters. Duplicate tags are rejected case-insensitively. Usernames must be 3–20 characters without whitespace; passwords must be at least 8 characters and nonblank. Validation failures return `400`. Handled client errors are logged at WARN; storage and search failures at ERROR.

Creation dates are stored as `LocalDate` and displayed as `dd.MM.yyyy`, without a time of day.

Example document request body:

```json
{
  "title": "Semester invoice",
  "type": "RECEIPT",
  "tags": ["university", "invoice"]
}
```

Supported document types are `RECEIPT`, `CONTRACT`, `BANK_STATEMENT`, `CERTIFICATE`, `NOTE`, `PASSPORT`, and `OTHER`.

## How to run
Note: Host ports `80`, `5432`, `5672`, `15672`, `9000`, `9001`, and `9200` must be available!

### 1. Configure the environment

Copy the template and replace every `REPLACE_ME` value:

```powershell
Copy-Item .env.example .env
```

The `.env` file is git-ignored. The database credentials must match the PostgreSQL service configuration.

#### GenAI key (Gemini)

The summaries are created by the GenAI worker with Google Gemini, which needs an API key:

1. Sign in at https://aistudio.google.com/app/apikey and create an API key.
2. Put it into `.env`: `SPRING_AI_GOOGLE_GENAI_API_KEY=<your key>` (never commit it; `.env` is git-ignored, `.env.example` only contains an empty placeholder).

The free tier is rate-limited (requests per minute and per day, the exact limits are shown in AI Studio and may change). When Gemini answers `429 Too Many Requests`, the worker waits and retries instead of failing right away; if the quota stays exhausted, the summary of that document is not created.

Without a key the application still works: upload, OCR and search are not affected. The GenAI worker logs an error for each document, and no summary is created (the AI tab then shows "Noch keine Zusammenfassung verfügbar").

### 2. Build and start all services

Start the application with Docker Compose from project root:

```powershell
docker compose up --build
```

To run in the background, use `docker compose up -d --build`. View logs with `docker compose logs -f paperless-rest ocr-worker`.

### Rebuild the search index (optional)

If the index was lost or Elasticsearch calls failed earlier, set `SEARCH_REINDEX_ON_STARTUP=true` in `.env` and restart the REST service (`docker compose up -d paperless-rest`). On startup all titles are rewritten from PostgreSQL; the OCR `text` stays untouched (it lives only in Elasticsearch). Set it back to `false` afterwards. Orphaned index entries are not removed; the search ignores IDs that no longer exist in PostgreSQL.

The index is only created if it does not exist yet, and its mapping is `dynamic: strict`. If your Elasticsearch volume was created before the `summary` field was added to the mapping, saving a summary fails. Run `docker compose down -v` once (this also deletes the PostgreSQL and Silo volumes) so the index is created again with the new mapping.

### 3. Open and use the application

| Service | Address |
| --- | --- |
| Web application | http://localhost |
| REST API through nginx | http://localhost/api |
| RabbitMQ management UI | http://localhost:15672 |
| Silo web console (login: `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`) | http://localhost:9001 |
| Elasticsearch REST API | http://localhost:9200 |

### 4. Try it: upload a PDF and search its content

The repository contains `hello-world.pdf` (text: "Hello World") for a quick end-to-end check of upload → Silo → RabbitMQ → OCR → Elasticsearch → search.

1. Open http://localhost, register a user and log in.
2. Upload `hello-world.pdf` with a title that does **not** contain "Hello", e.g. `Test document`.
3. Wait a few seconds for the OCR worker (`docker compose logs -f ocr-worker` shows `OCR completed for documentId=...`).
4. Search for `Hello` (or the typo `helo`, the search is fuzzy): the document appears, found by its OCR text and not by its title.

Optional check of the index: `curl http://localhost:9200/documents/_search?q=text:hello`


### How to stop the application

```powershell
docker compose down
```

## Testing strategy
### Existing tests

| Level | Tools | Coverage |
| --- | --- | --- |
| Service unit tests | JUnit, Mockito | Metadata defaults and type creation, event publication on save, no publication when repository save fails, and update behavior. |
| Search and storage tests | JUnit, Mockito | Elasticsearch service (search, title save, delete) with a mocked client, index initializer and reindex runner, after-commit index sync listener, storage service retry and bucket initializer. |
| Controller tests | Spring MVC test slices, MockMvc, Mockito | Registration/login happy paths, document listing/search delegation, metadata mapping, creation/update, username/title/tag validation, creation Location headers, empty-list 204 responses, and missing-document responses. |
| Frontend tests | Angular TestBed, Vitest | Tag validation, document HTTP requests including 204 handling, authentication, notifications, and document details. |

Next planned tests are Integration Tests for RabbitMQ!

Run the API tests from the project root (Java 25 required):

```powershell
.\api\mvnw.cmd -f api/pom.xml test
```

Run the OCR worker tests from the project root using the same wrapper:

```powershell
.\api\mvnw.cmd -f ocr-worker/pom.xml test
```

Application-context tests may require environment variables and running infrastructure. RabbitMQ startup unit tests cover explicit initialization, failure propagation, and API readiness; a fresh-broker end-to-end test is still outstanding.

For frontend development/testing, install Node.js 24 and run from `frontend/`:

```powershell
npm ci
npm test -- --watch=false
```

## Lessons learned
- Think of logging messages early and design them in a way they are easily traceable.
- Think of tests early!
