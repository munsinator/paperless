# Paperless

A document-management study project built with Angular, Spring Boot, PostgreSQL, and RabbitMQ.
The current implementation supports user registration and login, document metadata management, search, and asynchronous PDF OCR using Tesseract (English and German).

## Components 

| Component | Technology |
| --- | --- |
| PaperlessWebUI | Angular 21, TypeScript, Tailwind CSS |
| Web server / reverse proxy | nginx |
| PaperlessREST | Java 25, Spring Boot 4.1.1 |
| Database | PostgreSQL 16, Spring Data JPA |
| Message broker | RabbitMQ |
| OCR worker | Java 25, Spring Boot AMQP |
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
| POST | `/document` | Create metadata (JSON), or upload a PDF and trigger OCR (multipart) |
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
Generate a random JWT signing key, then replace the placeholder.

### 2. Build and start all services

Start the application with Docker Compose from project root:

```powershell
docker compose up --build
```

To run in the background, use `docker compose up -d --build`. View logs with `docker compose logs -f paperless-rest ocr-worker`.

### 3. Open and use the application

| Service | Address |
| --- | --- |
| Web application | http://localhost |
| REST API through nginx | http://localhost/api |
| RabbitMQ management UI | http://localhost:15672 |


### How to stop the application

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
## OCR and startup troubleshooting

Start with `docker compose up -d --build --wait`. The web UI now waits for the API health check.

PDF uploads are stored in the `document_data` volume. The API publishes a document ID after the database transaction commits; the worker reads `/data/documents/<id>.pdf`, renders each page with PDFBox, runs Tesseract, and publishes the result. The API saves the result in `ocrText`. Refresh the document after processing to see the text. Metadata-only JSON requests do not run OCR. Previously uploaded files were discarded, so upload those PDFs again.

The worker image includes Tesseract and English/German language data. For execution outside Docker, configure `documents.storage-path` identically in both Java applications and install Tesseract with `TESSDATA_PREFIX` pointing to its language data.

Registration failures have different causes:

- 502/503/504: the API is starting or unavailable. Previously nginx started before the API was ready. nginx now also refreshes Docker DNS after an API container replacement.
- 403 on browser POST requests: the old CORS configuration only allowed the Angular development origin (port 4200). Docker UI origins `http://localhost` and `http://127.0.0.1` are now allowed too.
- 409: the username already exists. Rebuilding images and `docker compose down` preserve the PostgreSQL volume and accounts; log in to an existing account.

Use `docker compose logs --tail 100 paperless-rest ocr-worker paperless-web-ui` for details. Do not use `down -v` to fix startup: it deletes stored users/documents and broker data.

RabbitMQ data is now persistent, and both services declare the job/result queues so startup order does not lose messages. Listener failures retry three times before rejection; inspect logs and re-upload if processing fails. Publication still has no transactional outbox, so an extended broker outage during upload can leave a document without an OCR job. File download/deletion endpoints remain unimplemented.
`node scripts/smoke-ocr.cjs` runs a local end-to-end check using `hello-world.pdf`: registration, duplicate rejection, login, upload, OCR persistence, and search. It creates a uniquely named test account and sample document.

### Tess4J im OCR-Container

Der Worker verwendet Tess4J 4.5.5 mit den nativen Tesseract-4.1.x-Bibliotheken von Ubuntu Jammy. PDFBox 3.0.8 rendert die PDF-Seiten; die Texterkennung erfolgt mit `Tesseract.doOCR(BufferedImage)` direkt aus Java. Ghost4J ist ausgeschlossen, weil die PDF-Konvertierung bereits PDFBox übernimmt. Ein Upgrade von Tess4J muss zu den nativen Tesseract-/Leptonica-Versionen im Image passen.
