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


## Architecture!

### Document processing flow

```
Upload ─► REST: save metadata in PostgreSQL ─► store PDF in Silo ─► index title in Elasticsearch
            └─► after commit: DocumentSavedEvent ─► exchange document.events
                  └─► OCR worker: load PDF from Silo, run Tesseract
                        └─► OcrCompletedEvent ─► exchange ocr.completed
                              └─► indexing worker: add OCR text to the Elasticsearch entry
Search ─► REST ─► Elasticsearch (ranked document IDs) ─► load documents from PostgreSQL
```

PostgreSQL is the source of truth; Elasticsearch is only the search view. The REST service owns the `title` in the index (upload, rename, delete); the indexing worker only adds `text`.

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
| Upload event | exchange `document.events` (fanout) → queue `ocr.document.events`, payload `{ documentId }` |
| OCR result event | exchange `ocr.completed` (fanout) → queue `indexing.ocr.completed`, payload `{ documentId, text }` |
| Search index | `documents`, fields `title` and `text`, Elasticsearch `_id` = document ID |

### REST interface

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

If the storage is unavailable, the upload fails with `503` and nothing is persisted (the database transaction is rolled back). Uploads are limited to 15 MB.

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
Note: Host ports `80`, `5432`, `5672`, `15672`, `9000`, `9001`, and `9200` must be available!

### 1. Configure the environment

Copy the template and replace every `REPLACE_ME` value:

```powershell
Copy-Item .env.example .env
```

The `.env` file is git-ignored. The database credentials must match the PostgreSQL service configuration.

### 2. Build and start all services

Start the application with Docker Compose from project root:

```powershell
docker compose up --build
```

To run in the background, use `docker compose up -d --build`. View logs with `docker compose logs -f paperless-rest ocr-worker`.

### Rebuild the search index (optional)

If the index was lost or Elasticsearch calls failed earlier, set `SEARCH_REINDEX_ON_STARTUP=true` in `.env` and restart the REST service (`docker compose up -d paperless-rest`). On startup all titles are rewritten from PostgreSQL; the OCR `text` stays untouched (it lives only in Elasticsearch). Set it back to `false` afterwards. Orphaned index entries are not removed; the search ignores IDs that no longer exist in PostgreSQL.

### 3. Open and use the application

| Service | Address |
| --- | --- |
| Web application | http://localhost |
| REST API through nginx | http://localhost/api |
| RabbitMQ management UI | http://localhost:15672 |
| Silo web console (login: `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`) | http://localhost:9001 |
| Elasticsearch REST API | http://localhost:9200 |


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